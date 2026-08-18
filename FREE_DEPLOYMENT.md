# Free Cloud Deployment Guide: Collaborative C++ Code Editor

This step-by-step guide explains how to deploy your collaborative editor to the cloud **completely for free** using **Oracle Cloud Always Free Tier** (which offers a powerful 4-core, 24GB RAM VPS) and **DuckDNS** (for a free SSL-secured domain).

---

## Part 1: Set Up Your Free Cloud Server (Oracle Cloud)

Oracle Cloud Infrastructure (OCI) offers a generous "Always Free" tier.

### Step 1: Create an Account
1. Go to [oracle.com/cloud/free](https://www.oracle.com/cloud/free/) and sign up.
2. *Note: You will need to provide a credit/debit card to verify identity. Oracle will make a temporary authorization hold (around $1 USD) and immediately refund it. You will not be charged unless you manually upgrade to a paid account.*

### Step 2: Create a Compute Instance (VM)
1. Once logged into the Oracle Cloud Console, click **Create a VM instance**.
2. **Name:** Set a name (e.g., `coderoom-server`).
3. **Placement:** Keep default settings.
4. **Image and Shape:**
   - Click **Edit**.
   - **Image:** Select **Canonical Ubuntu** (choose the latest version, e.g., Ubuntu 22.04 or 24.04).
   - **Shape:** Click **Change Shape**. Select **Ampere (ARM-based processor)**, then check **VM.Standard.A1.Flex**.
   - Configure: **2 OCPUs** and **12 GB RAM** (this fits perfectly within your free 4-OCPU/24GB limit, leaving room for other projects if needed).
5. **Networking:**
   - Keep default "Create new Virtual Cloud Network (VCN)" selected.
   - Make sure **Assign a public IPv4 address** is set to **Yes**.
6. **SSH Keys:**
   - Click **Save private key** to download the `.key` (or `.pem`) file. You will need this to log into your server.
7. **Boot Volume:** Keep defaults.
8. Click **Create** (it takes 1-2 minutes to provision). Note down the **Public IP Address** when it status changes to *Running*.

### Step 3: Open Ports in Oracle Firewall (VCN Ingress Rules)
By default, Oracle blocks incoming traffic. You must open ports 80 (HTTP) and 443 (HTTPS):
1. In your VM instance details page, under **Instance access**, click on the **Virtual cloud network** link.
2. Click on the **Security Lists** in the left menu, then click on the **Default Security List**.
3. Click **Add Ingress Rules**:
   - **Source CIDR:** `0.0.0.0/0`
   - **IP Protocol:** `TCP`
   - **Destination Port Range:** `80,443`
   - **Description:** `HTTP and HTTPS traffic`
4. Click **Add Ingress Rules**.

---

## Part 2: Get a Free Domain & Point to Your VM (DuckDNS)

Since Let's Encrypt requires a valid domain name to generate an SSL certificate, you can get a free subdomain using DuckDNS.

### Step 1: Register Subdomain
1. Go to [duckdns.org](https://www.duckdns.org/) and sign in using Reddit, GitHub, Google, or Twitter.
2. In the **subdomains** input box, type your desired subdomain name (e.g., `mycoderoom`) and click **add domain**.
3. Your domain will be: `mycoderoom.duckdns.org`.

### Step 2: Point Domain to your Oracle Public IP
1. Copy the **Public IP Address** of your Oracle VM instance.
2. In the DuckDNS console, find your subdomain, paste your IP address in the **ip** box, and click **update ip**.

---

## Part 3: Access and Configure Your VM

### Step 1: Connect to your Server
Open your computer's terminal (or Git Bash on Windows) and log in using the downloaded SSH private key:
```bash
# Set correct permissions on the private key file (Mac/Linux only)
chmod 400 /path/to/ssh-key.key

# Connect to the server
ssh -i /path/to/ssh-key.key ubuntu@YOUR_ORACLE_PUBLIC_IP
```

### Step 2: Install Docker and Docker Compose
Run the following commands on your server to install Docker:
```bash
# Update Ubuntu package database
sudo apt update && sudo apt upgrade -y

# Install Docker and git
sudo apt install -y docker.io docker-compose-v2 git

# Add the ubuntu user to the docker group so you don't need 'sudo' for docker commands
sudo usermod -aG docker ubuntu

# Log out and log back in to apply group changes
exit
```
Log back in:
```bash
ssh -i /path/to/ssh-key.key ubuntu@YOUR_ORACLE_PUBLIC_IP
```

### Step 3: Set Up Swap Memory
While your VM has plenty of RAM (12GB), it is always best practice to configure 2GB of Swap memory to prevent processes from crashing under spikes:
```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

---

## Part 4: Deploy Your Application

### Step 1: Clone Your Project
```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd CodeRoom---A-real-time-collaborative-C-code-editor
```

### Step 2: Build the Sandbox GCC Runner Image
Build the GCC base image on the host OS:
```bash
docker build -t cpp-runner docker/cpp-runner
```

### Step 3: Configure Environment Variables
Create a production environment file:
```bash
# Generate random passwords for security
DB_PASS=$(openssl rand -hex 16)
JWT_SEC=$(openssl rand -hex 32)

# Write configuration
cat <<EOF > .env
DATABASE_NAME=collab_editor
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=$DB_PASS
JWT_SECRET=$JWT_SEC
EOF
```

### Step 4: Configure your Nginx Domain
Open Nginx configuration file:
```bash
nano docker/nginx/nginx.conf
```
Replace all occurrences of `yourdomain.com` with your DuckDNS domain (e.g., `mycoderoom.duckdns.org`). Save and close (Press `Ctrl+O`, `Enter`, then `Ctrl+X`).

---

## Part 5: Generate Free SSL Certificates

To ensure web browsers load the collaborative editor over a secure connection, setup Let's Encrypt certificates:

### Step 1: Obtain the Certificates
Run Certbot to request free certificates:
```bash
sudo apt install certbot -y
sudo certbot certonly --standalone -d YOUR_SUBDOMAIN.duckdns.org
```
*(Enter your email address and agree to the terms when prompted).*

### Step 2: Move Certificates to Nginx Directories
Create a directory in your project to mount certificates into Nginx:
```bash
mkdir -p docker/nginx/certs

# Copy the certificates into your project folder
sudo cp /etc/letsencrypt/live/YOUR_SUBDOMAIN.duckdns.org/fullchain.pem docker/nginx/certs/
sudo cp /etc/letsencrypt/live/YOUR_SUBDOMAIN.duckdns.org/privkey.pem docker/nginx/certs/

# Change ownership of these copied certs so Nginx container can read them
sudo chown -R ubuntu:ubuntu docker/nginx/certs
```

---

## Part 6: Start the Stack!

Launch all services in background daemon mode:
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### Verify Status
Check if all containers are running successfully:
```bash
docker compose -f docker-compose.prod.yml ps
```

If everything is active, open your browser and navigate to:
**`https://YOUR_SUBDOMAIN.duckdns.org`**

Your real-time collaborative editor is now officially live on the internet, secure, and ready to be placed on your resume!
