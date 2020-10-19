# Setting up the cardano e-commerce app for shopify on your own server.

**This is work in progress with lots of scattered bits and pieces. Don't try following it yet.**

## Enable Port forwarding:

Run the following commands make the endpoints available to the world:
 
```
# Clear all rules

sudo iptables -P INPUT ACCEPT
sudo iptables -P FORWARD ACCEPT
sudo iptables -P OUTPUT ACCEPT
sudo iptables -t nat -F
sudo iptables -t mangle -F
sudo iptables -F
sudo iptables -X

sudo ip6tables -P INPUT ACCEPT
sudo ip6tables -P FORWARD ACCEPT
sudo ip6tables -P OUTPUT ACCEPT
sudo ip6tables -t nat -F
sudo ip6tables -t mangle -F
sudo ip6tables -F
sudo ip6tables -X

# Add this rule for SSH (REMOVE if the app will run from your own desktop)
sudo iptables -A INPUT -p tcp -m tcp --dport 22 -j ACCEPT

# Add the following rules to redirect traffic (HTTP and HTTPS) to ports 8788 and 8787 
sudo iptables -A INPUT -p tcp -m tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp -m tcp --dport 443 -j ACCEPT
sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to 8788
sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to 8787
sudo iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP
sudo iptables -A INPUT -p tcp ! --syn -m state --state NEW -j DROP
sudo iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP
sudo iptables -P OUTPUT ACCEPT

```

Add the commands above (without sudo) to `/etc/rc.local` to make this setting survive server restarts:

If the file doesn't exist create it with:

```
printf '%s\n' '#!/bin/bash' 'exit 0' | sudo tee -a /etc/rc.local
sudo chmod +x /etc/rc.local
sudo nano /etc/rc.local
```

It should at least have the following:
```
#!/bin/bash

# Add this rule for SSH (REMOVE if the app will run from your own desktop)
iptables -A INPUT -p tcp -m tcp --dport 22 -j ACCEPT

# Add the following rules to redirect traffic (HTTP and HTTPS) to ports 8788 and 8787
iptables -A INPUT -p tcp -m tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp -m tcp --dport 443 -j ACCEPT
iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to 8788
iptables -t nat -A PREROUTING -p tcp --dport 443 -j REDIRECT --to 8787
iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP
iptables -A INPUT -p tcp ! --syn -m state --state NEW -j DROP
iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP
iptables -P OUTPUT ACCEPT

exit 0
```

Then reboot and check rules with `sudo iptables -t nat -L`.

If for some reason the firewall goes crazy, remove UFW.
```
sudo ufw disable
sudo apt-get remove ufw
sudo apt-get purge ufw
```

Make sure you are visible to the internet:

```
# The commands also work if you have a domain name, make sure you have the following host records:

Record type    Host name     Address
----------------------------------------------------
CNAME          www           your.domain
CNAME          *             your.domain
A              @             <YOUR_IP_ADDRESS>
URL Frame      cardano       <YOUR_IP_ADDRESS>:8788


# test HTTP connections
nc -zvw3 <YOUR_IP_ADDRESS> 80

# if you are using a domain name
nc -zvw3 your.domain 80

# test HTTPS connections
nc -zvw3 <YOUR_IP_ADDRESS> 443

# if you are using a domain name
nc -zvw3 your.domain 80
```

That's all you need to do if you are running the server from your own desktop.

If using an actual server, read on.

## Create an application user

Create a new user with a locked account just for the application:

```
# creates a user named 'cardano' that has no password and no home directory. It cannot be logged into.

sudo useradd -M -s /bin/false cardano
```

To start an application under this user, use `sudo`:
```
sudo -u cardano /usr/local/cardano/shopify-cardano-server/run.sh
```
