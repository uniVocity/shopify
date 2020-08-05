```
cd ~/pool-keys/

#paste wallet address in this file:
nano target-wallet.addr

# AS ALWAYS, MAKE A TEST TRANSACTION FIRST BEFORE MOVING A LOT OF CASH

# Get protocol params:
cardano-cli shelley query protocol-parameters \
   --mainnet \
   --out-file /tmp/params.json 

#Find your balance and UTXOs:
cardano-cli shelley query utxo \
   --address $(cat payment.addr) \
   --mainnet 
 > /tmp/fullUtxo.out

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

# deposit 10000000 lovelace (10 ADA)
amount=10000000

#Draft the transaction
cardano-cli shelley transaction build-raw \
    ${tx_in} \
    --tx-out $(cat target-wallet.addr)+0 \
    --tx-out $(cat payment.addr)+0 \
    --ttl 0 \
    --fee 0 \
    --out-file /tmp/tx.draft

#Calculate the current minimum fee:
fee=$(cardano-cli shelley transaction calculate-min-fee \
    --tx-body-file /tmp/tx.draft \
    --tx-in-count 1 \
    --tx-out-count 2 \
    --witness-count 1 \
    --byron-witness-count 0 \
   --mainnet \
    --protocol-params-file /tmp/params.json | awk '{ print $1 }')
echo fee: $fee


#Calculate your change output.
txOut=$((${total_balance}-${amount}-${fee}))
echo Change Output: ${txOut}

# Find the tip of the blockchain:
currentSlot=$(cardano-cli shelley query tip--mainnet | jq -r '.slotNo')
echo Current Slot: $currentSlot

# Build the transaction
cardano-cli shelley transaction build-raw \
 ${tx_in} \
 --tx-out $(cat target-wallet.addr)+${amount} \
 --tx-out $(cat payment.addr)+${txOut} \
 --ttl $((${currentSlot} + 10000)) \
 --fee ${fee} \
 --out-file /tmp/tx.raw

#Sign the transaction
cardano-cli shelley transaction sign \
--tx-body-file /tmp/tx.raw \
--signing-key-file payment.skey \
--mainnet \
--out-file /tmp/tx.signed

#Send the signed transaction.
cardano-cli shelley transaction submit \
   --tx-file /tmp/tx.signed \
   --mainnet 


#Check balance of wallet that sent the amount
cardano-cli shelley query utxo \
   --address $(cat payment.addr) \
   --mainnet

#Check balance of target wallet
cardano-cli shelley query utxo \
   --address $(cat target-wallet.addr) \
   --mainnet

```
