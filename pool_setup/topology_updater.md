## Using the topologyUpdater.sh

Once again, save this file locally on your computer and from your favorite editor run a Search+Replace on it:

```
<USER>
<THIS_RELAY_PUBLIC_IP>

# Here replace the IPs with either the private network IP of your block node and/or relays
<BLOCK_NODE_IP>
<YOUR_OTHER_RELAY_IP_ADDRESS>
```

Do this for each relay node you want to run.

Then log into your relay nodes.
```
ssh -A <USER>@<THIS_RELAY_PUBLIC_IP>
```

Create the script:

```
cat > ~/topologyUpdater.sh << EOF
#!/bin/bash
# shellcheck disable=SC2086,SC2034
 
USERNAME="\$(whoami)"
CNODE_PORT=3001  # must match your relay node port as set in the startup command
CNODE_HOSTNAME="<THIS_RELAY_PUBLIC_IP>"  # must resolve to the public IP of the relay
CNODE_BIN="\$/usr/local/bin/"
CNODE_HOME=/home/<USER>/cardano-node
CNODE_LOG_DIR="\${CNODE_HOME}/logs/"
GENESIS_JSON="/home/<USER>/config/mainnet-shelley-genesis.json"
NETWORKID=\$(jq -r .networkId \$GENESIS_JSON)
CNODE_VALENCY=1   # optional for multi-IP hostnames
NWMAGIC=\$(jq -r .networkMagic < \$GENESIS_JSON)
[[ "\${NETWORKID}" = "Mainnet" ]] && HASH_IDENTIFIER="--mainnet" || HASH_IDENTIFIER="--testnet-magic \${NWMAGIC}"
[[ "\${NWMAGIC}" = "764824073" ]] && NETWORK_IDENTIFIER="--mainnet" || NETWORK_IDENTIFIER="--testnet-magic \${NWMAGIC}"
 
export PATH="\${CNODE_BIN}:\${PATH}"
export CARDANO_NODE_SOCKET_PATH="\${CNODE_HOME}/db/node.socket"
 
blockNo=\$(cardano-cli shelley query tip --mainnet | jq -r .blockNo )
 
# Note:
# if you run your node in IPv4/IPv6 dual stack network configuration and want announced the
# IPv4 address only please add the -4 parameter to the curl command below  (curl -4 -s ...)
if [ "\${CNODE_HOSTNAME}" != "CHANGE ME" ]; then
  T_HOSTNAME="&hostname=\${CNODE_HOSTNAME}"
else
  T_HOSTNAME=''
fi

if [ ! -d \${CNODE_LOG_DIR} ]; then
  mkdir -p \${CNODE_LOG_DIR};
fi
 
curl -4 -s "https://api.clio.one/htopology/v1/?port=\${CNODE_PORT}&blockNo=\${blockNo}&valency=\${CNODE_VALENCY}&magic=\${NWMAGIC}\${T_HOSTNAME}" | tee -a \$CNODE_LOG_DIR/topologyUpdater_lastresult.json
EOF

```

Add permissions and run the updater script.
```
chmod +x ~/topologyUpdater.sh
~/topologyUpdater.sh
```


When the topologyUpdater.sh runs successfully, you will see
``` 
{ "resultcode": "201", "datetime":"2020-07-28 01:23:45", "clientIp": "1.2.3.4", "iptype": 4, "msg": "nice to meet you" }
```

Add a crontab job to automatically run topologyUpdater.sh every hour on the 22nd minute. You can change the 22 value to your own preference:

```
cat > /home/<USER>/crontab-fragment.txt << EOF
22 * * * * . $HOME/.profile; /home/<USER>/topologyUpdater.sh
EOF
crontab -l | cat - crontab-fragment.txt > crontab.txt && crontab crontab.txt

# you might get a "no crontab for [user]". In this case run:
crontab -e

rm crontab-fragment.txt

# Check if the crontab rule is active:
crontab -l

```


## Update relay node topology files:

After **four hours** since you execute the steps in the previous section:

```
# Check if the topology updater ran OK with crontab:
less ~/cardano-node/logs/topologyUpdater_lastresult.json 
```
 
When your relay node IP is properly registered, run this:

RELAY1 (assuming block node is in the same private network than relay 1)
```
cat > ~/relay-topology_pull.sh << EOF
#!/bin/bash
cp ~/config/mainnet-topology.json ~/config/old_mainnet-topology.json
curl -4 -s -o ~/config/new-mainnet-topology.json "https://api.clio.one/htopology/v1/fetch/?max=20&customPeers=<BLOCK_NODE_IP>:3001:2|<YOUR_OTHER_RELAY_IP_ADDRESS>:3001:2|relays-new.cardano-mainnet.iohk.io:3001:2"
EOF
```

Add permissions and pull new topology files.
```
chmod +x ~/relay-topology_pull.sh
~/relay-topology_pull.sh

# Grab the suggested topology from here and update the current topology manually. I
# suggest you to checking if you can connect to the IPs in this file.
less ~/config/new-mainnet-topology.json

#If you changed the topology (in ~/config/mainnet-topology.json), these modifications only 
take effect after restarting your stake pool.
sudo systemctl restart cardano
```
