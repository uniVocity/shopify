# Block node setup

First, make sure you completed all steps listed in the [base setup guide](./base_setup.md).

For added security, you can also execute the [base setup guide](./base_setup.md) on an air-gapped
computer, formatted with a new Ubuntu installation and free of any additional software.

Once again, save this file locally on your computer and from your favorite editor run a Search+Replace on it:
```
<USER>
<YOUR_POOL_NAME>
<YOUR_BLOCK_NODE_IP_ADDRESS>
<YOUR_RELAY_IP_ADDRESS>

# these are optional, use if you want to keep your pool metadata in a github page
<YOUR_GITHUB_USER>
<YOUR_GITHUB_PROJECT>
```
# login to block node
```
ssh -A <USER>@<YOUR_BLOCK_NODE_IP_ADDRESS>
```

## Block producer keys

 * If you are running commands from an air-gapped PC, disconnect it from the internet once you are able to run `cardano-cli`.

 * Create a directory on your local machine and node server to store your keys:

Execute:
```
mkdir ~/pool-keys
cd ~/pool-keys

# Make a KES key pair.
cardano-cli shelley node key-gen-KES --verification-key-file kes.vkey --signing-key-file kes.skey

# Make a set of cold keys and create the cold counter file.
cardano-cli shelley node key-gen  --cold-verification-key-file cold.vkey --cold-signing-key-file cold.skey --operational-certificate-issue-counter-file cold.counter

# Make a VRF key pair.
cardano-cli shelley node key-gen-VRF --verification-key-file vrf.vkey --signing-key-file vrf.skey

# Determine the number of slots per KES period from the genesis file.
slotsPerKESPeriod=$(cat ~/config/mainnet-shelley-genesis.json | jq -r '.slotsPerKESPeriod')
echo slotsPerKESPeriod: ${slotsPerKESPeriod}

# Determine latest slot tip (should match what https://htn.pooltool.io/ gives)
slotNo=$(cardano-cli shelley query tip --mainnet | jq -r '.slotNo')
echo slotNo: ${slotNo}

# Find the kesPeriod by dividing the slot tip number by the slotsPerKESPeriod
kesPeriod=$((${slotNo} / ${slotsPerKESPeriod}))
echo kesPeriod: ${kesPeriod}

# With this calculation, you can generate a operational certificate for your pool. 
cardano-cli shelley node issue-op-cert --kes-verification-key-file kes.vkey --cold-signing-key-file cold.skey --operational-certificate-issue-counter cold.counter \
--kes-period $kesPeriod \
--out-file node.cert

# Make a VRF key pair.
cardano-cli shelley node key-gen-VRF \
    --verification-key-file vrf.vkey \
    --signing-key-file vrf.skey
```

## Setup payment and staking keys

Make sure the node is in sync, then execute:

```
cd ~/pool-keys

# Create a new payment key pair:  payment.skey & payment.vkey
cardano-cli shelley address key-gen \
    --verification-key-file payment.vkey \
    --signing-key-file payment.skey

# Create a new stake address key pair
cardano-cli shelley stake-address key-gen \
    --verification-key-file stake.vkey \
    --signing-key-file stake.skey

# Create your stake address from the stake address verification key and store it in stake.addr
cardano-cli shelley stake-address build \
    --staking-verification-key-file stake.vkey \
    --out-file stake.addr \
    --mainnet

# Build a payment address for the payment key payment.vkey which will delegate to the stake address, stake.vkey
cardano-cli shelley address build \
    --payment-verification-key-file payment.vkey \
    --staking-verification-key-file stake.vkey \
    --out-file payment.addr \
    --mainnet

```


## Register a stake address
```
cardano-cli shelley stake-address registration-certificate \
    --staking-verification-key-file stake.vkey \
    --out-file stake.cert


# Fund payment address. First get the address with:
cat payment.addr

# Then send your pledge amount + some extra ada for pool registration fees from Daedalus.
# BE CAREFUL TO TEST WITH A SMALL AMOUNT FIRST, AND PLEASE TEST THAT YOU CAN GET THE FUNDS BACK
# AND RECHECK MULTIPLE TIMES THAT YOU HAVE THE payment.skey FILE AVAILABLE 
# IN MORE THAN ONE BACKUP. AND THAT YOU CAN RESTORE THE BACKUP YOU HAVE. 
# AND DONT LEAVE THE payment.skey FILE IN THE SERVER!
# Refer to file send_simple_payment.md to test and make sure you are able to move the funds around.

# Check your payment address balance:
cardano-cli shelley query utxo \
    --address $(cat payment.addr) \
    --mainnet 

# Find the tip of the blockchain to set the ttl parameter properly.
currentSlot=$(cardano-cli shelley query tip --mainnet | jq -r '.slotNo')
echo Current Slot: $currentSlot

#Find your balance and UTXOs:
cardano-cli shelley query utxo \
    --address $(cat payment.addr) \
    --mainnet > /tmp/fullUtxo.out

tail -n +3 /tmp/fullUtxo.out | sort -k3 -nr > /tmp/balance.out

# Here is your balance
cat /tmp/balance.out

tx_in=""
total_balance=0
while read -r utxo; do
    in_addr=$(awk '{ print $1 }' <<< "${utxo}")
    idx=$(awk '{ print $2 }' <<< "${utxo}")
    utxo_balance=$(awk '{ print $3 }' <<< "${utxo}")
    total_balance=$((${total_balance}+${utxo_balance}))
    echo TxHash: ${in_addr}#${idx}
    echo ADA: ${utxo_balance}
    tx_in="${tx_in} --tx-in ${in_addr}#${idx}"
done < /tmp/balance.out
txcnt=$(cat /tmp/balance.out | wc -l)
echo Total ADA balance: ${total_balance}
echo Number of UTXOs: ${txcnt}

# Obtain the protocol-parameters:
cardano-cli shelley query protocol-parameters \
    --mainnet \
    --out-file /tmp/params.json

#Find the keyDeposit value.
keyDeposit=$(cat /tmp/params.json | jq -r '.keyDeposit')
echo keyDeposit: $keyDeposit
```

keyDeposit produces the cost of the registration of a stake address certificate, in lovelace.

```
#Run the build-raw transaction command (using slot+10000 for TTL)
cardano-cli shelley transaction build-raw \
    ${tx_in} \
    --tx-out $(cat payment.addr)+0 \
    --ttl $(( ${currentSlot} + 10000)) \
    --fee 0 \
    --out-file /tmp/tx.tmp \
    --certificate stake.cert

#Calculate the current minimum fee:
fee=$(cardano-cli shelley transaction calculate-min-fee \
    --tx-body-file /tmp/tx.tmp \
    --tx-in-count ${txcnt} \
    --tx-out-count 1 \
    --mainnet \
    --witness-count 2 \
    --byron-witness-count 0 \
    --protocol-params-file /tmp/params.json | awk '{ print $1 }')
echo fee: $fee

#Calculate your change output.
txOut=$((${total_balance}-${keyDeposit}-${fee}))
echo Change Output: ${txOut}

#Build your transaction which will register your stake address.
cardano-cli shelley transaction build-raw \
    ${tx_in} \
    --tx-out $(cat payment.addr)+${txOut} \
    --ttl $(( ${currentSlot} + 10000)) \
    --fee ${fee} \
    --certificate-file stake.cert \
    --out-file /tmp/tx.raw

#Sign the transaction with both the payment and stake secret keys.
cardano-cli shelley transaction sign \
    --tx-body-file /tmp/tx.raw \
    --signing-key-file payment.skey \
    --signing-key-file stake.skey \
    --mainnet \
    --out-file /tmp/tx.signed

#Send the signed transaction.
cardano-cli shelley transaction submit \
    --tx-file /tmp/tx.signed \
    --mainnet 
    

```

## Register your stake pool

You need to host a metadata file somewhere on the internet. The easiest thing is to
create a project on github and drop it there.

The metadata is simply a JSON file with the following format: 

```json
{
"name": "YOUR POOL NAME",
"description": "MY POOL IS COOL.",
"ticker": "TICKER",
"homepage": "https://github.com/<YOUR_GITHUB_USER>/<YOUR_GITHUB_PROJECT>"
} 
```

Once it's available online, download that file to register your pool:

```
#Get the pool metadata file:
cd /tmp/
wget https://raw.githubusercontent.com/<YOUR_GITHUB_USER>/<YOUR_GITHUB_PROJECT>/master/<YOUR_POOL_NAME>.json

#Calculate the hash of your metadata file.
cardano-cli shelley stake-pool metadata-hash --pool-metadata-file <YOUR_POOL_NAME>.json > <YOUR_POOL_NAME>Hash.txt

minPoolCost=$(cat /tmp/params.json | jq -r .minPoolCost)
echo minPoolCost: ${minPoolCost}

# Create a registration certificate for your stake pool. 
# Here we are pledging 10,000 ADA with a fixed pool cost of 340 ADA and a pool margin of 2%.
# VERY IMPORTANT, the --metadata-url value has to have at most 64 characters in length. If you
# can't make it happen you can use a URL shortener such as bit.ly or something. 

cardano-cli shelley stake-pool registration-certificate \
    --cold-verification-key-file ~/pool-keys/node.vkey \
    --vrf-verification-key-file ~/pool-keys/vrf.vkey \
    --pool-pledge 10000000000 \
    --pool-cost 340000000 \
    --pool-margin 0.02 \
    --pool-reward-account-verification-key-file ~/pool-keys/stake.vkey \
    --pool-owner-stake-verification-key-file ~/pool-keys/stake.vkey \
    --mainnet \
    --pool-relay-ipv4 <YOUR_RELAY_IP_ADDRESS>  \
    --pool-relay-port 3001 \
    --pool-relay-ipv4 <YOUR_RELAY_IP_ADDRESS> \
    --pool-relay-port 3001 \
    --metadata-url https://<YOUR_GITHUB_USER>.github.io/<YOUR_GITHUB_PROJECT>/<YOUR_POOL_NAME>.json \
    --metadata-hash $(cat <YOUR_POOL_NAME>Hash.txt) \
    --out-file ~/pool-keys/pool.cert

#Pledge stake to your stake pool.

cardano-cli shelley stake-address delegation-certificate \
    --staking-verification-key-file ~/pool-keys/stake.vkey \
    --cold-verification-key-file ~/pool-keys/node.vkey \
    --out-file ~/pool-keys/deleg.cert

cd ~/pool-keys

# Find the tip of the blockchain again:
currentSlot=$(cardano-cli shelley query tip --mainnet | jq -r '.slotNo')
echo Current Slot: $currentSlot

# Find balance and utxo
cardano-cli shelley query utxo \
    --address $(cat payment.addr) \
    --mainnet > /tmp/fullUtxo.out

tail -n +3 /tmp/fullUtxo.out | sort -k3 -nr >  /tmp/balance.out

cat  /tmp/balance.out

tx_in=""
total_balance=0
while read -r utxo; do
    in_addr=$(awk '{ print $1 }' <<< "${utxo}")
    idx=$(awk '{ print $2 }' <<< "${utxo}")
    utxo_balance=$(awk '{ print $3 }' <<< "${utxo}")
    total_balance=$((${total_balance}+${utxo_balance}))
    echo TxHash: ${in_addr}#${idx}
    echo ADA: ${utxo_balance}
    tx_in="${tx_in} --tx-in ${in_addr}#${idx}"
done <  /tmp/balance.out
txcnt=$(cat  /tmp/balance.out | wc -l)
echo Total ADA balance: ${total_balance}
echo Number of UTXOs: ${txcnt}

# Find the deposit fee for a pool.
poolDeposit=$(cat /tmp/params.json | jq -r '.poolDeposit')
echo poolDeposit: $poolDeposit

# Run the build-raw transaction command.
cardano-cli shelley transaction build-raw \
    ${tx_in} \
    --tx-out $(cat payment.addr)+$(( ${total_balance} - ${poolDeposit}))  \
    --ttl $(( ${currentSlot} + 10000)) \
    --fee 0 \
    --certificate-file pool.cert \
    --certificate-file deleg.cert \
    --out-file /tmp/tx.tmp

#Calculate the minimum fee:
fee=$(cardano-cli shelley transaction calculate-min-fee \
    --tx-body-file /tmp/tx.tmp \
    --tx-in-count ${txcnt} \
    --tx-out-count 1 \
    --mainnet \
    --witness-count 3 \
    --byron-witness-count 0 \
    --protocol-params-file /tmp/params.json | awk '{ print $1 }')
echo fee: $fee

#Calculate your change output:
txOut=$((${total_balance}-${poolDeposit}-${fee}))
echo txOut: ${txOut}

#Build the transaction
cardano-cli shelley transaction build-raw \
    ${tx_in} \
    --tx-out $(cat payment.addr)+${txOut} \
    --ttl $(( ${currentSlot} + 10000)) \
    --fee ${fee} \
    --certificate-file pool.cert \
    --certificate-file deleg.cert \
    --out-file /tmp/tx.raw

#Sign the transaction
cardano-cli shelley transaction sign \
    --tx-body-file /tmp/tx.raw \
    --signing-key-file ~/pool-keys/payment.skey \
    --signing-key-file ~/pool-keys/node.skey \
    --signing-key-file ~/pool-keys/stake.skey \
    --mainnet \
    --out-file /tmp/tx.signed

#Send the transaction
cardano-cli shelley transaction submit \
    --tx-file /tmp/tx.signed \
    --mainnet 

```


## encrypt and save keys

```
sudo apt-get install p7zip-full

cd ~/pool-keys

# encrypt with 7z:
7z a -p poolkeys.7z *
mkdir /home/<USER>/safe/
mv poolkeys.7z /home/<USER>/safe/
cd /home/<USER>/safe/

# if you want, you can split this to 'n' files, 2kb each part
split -b2k poolkeys.7z poolkeys_part.

# Move to safe locations, make multiple backups, tatoo the keys to your thigh.
# Keep ALL *cold* files offline, make multiple backups, make sure you can restore from these backups, then remove any trace of these files from any computer.
 
# If you generated the keys in your block node itself, copy the 7z file to your safe location:
scp <USER>@<YOUR_BLOCK_NODE_IP_ADDRESS>:/home/<USER>/pool-keys/poolkeys.7z /home/<USER>/safe
cd /home/<USER>/safe/


# to decrypt:
# recombine parts if you split the file:
cat poolkeys_part* > poolkeys.7z

# extract all keys
7z e poolkeys.7z

# prepare to move the required files to the block-node server:

mkdir serverkeys
mv deleg.cert kes.skey kes.vkey node.cert node.counter node.vkey payment.addr payment.vkey pool.cert stake.addr stake.cert stake.vkey vrf.skey vrf.vkey serverkeys/
cd serverkeys
7z a -p serverkeys.7z * 

```

Then, copy all verification keys and certificates to the block producing node (NOT the relays):

```
scp serverkeys.7z <USER>@<YOUR_BLOCK_NODE_IP_ADDRESS>:/home/<USER>/pool-keys

cd ~/pool-keys/
7z e serverkeys.7z
rm serverkeys.7z
ls ~/pool-keys/

# output:
> deleg.cert  kes.skey  kes.vkey  node.cert  node.counter  node.vkey  payment.addr  payment.vkey  pool.cert  stake.addr  stake.cert  stake.vkey  vrf.skey  vrf.vkey

```


# On your block-node server, stop the node
```

sudo systemctl stop cardano

# update run.sh:
nano ~/run.sh

# Add the following parameters to the startup command to include the keys and cert

   --shelley-kes-key ~/pool-keys/kes.skey --shelley-vrf-key ~/pool-keys/vrf.skey --shelley-operational-certificate ~/pool-keys/node.cert

# start pool
sudo systemctl start cardano

# check if it's all good
sudo journalctl -xe

# create the cold counter file (used later for pool registration)
cardano-cli shelley node key-gen \
    --cold-verification-key-file node.vkey \
    --cold-signing-key-file node.skey \
    --operational-certificate-issue-counter node.counter
```

## Locate your pool on the blockchain and check if it is working
```
cardano-cli shelley stake-pool id --verification-key-file ~/pool-keys/node.vkey > /tmp/stakepoolid.txt
cat /tmp/stakepoolid.txt

#verify it's included in the blockchain.
cardano-cli shelley query ledger-state --mainnet | grep publicKey | grep $(cat /tmp/stakepoolid.txt)

> output:
  "publicKey": "<same hash you got when you ran `cat /tmp/stakepoolid.txt`>",

```

Your node has been registered and should show up in daedalus.


