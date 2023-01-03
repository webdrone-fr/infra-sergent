# Update Sergent

## Update with sergent command

In meveo edit the CEI of your server and you can click on the button "Manage Sergent" and follow steps of [this command](./commands.md#manage-sergent)

## Update with creating a new sergent server image

For some reason you may also need to create a new server image on which the new environments will be based  

### I. New Sergent image

[Create a new sergent image](./dockerImage.md#release-a-new-sergent-image).  
On your desktop you can execute this docker command to create a sergent archive :  
`docker run --rm --entrypoint cat webdronesas/sergent:v1-beta /work/application > /mnt/c/sergent/sergent` (replace /mnt/c/.. with a path on your desktop)  
This command will create a file called "sergent" on your desktop : `/mnt/c/sergent/sergent` in this case.

### II. Create a new server

Server configuration with OVH :  
```sh
"name" : "sergent-image",
"imageRef" : "Debian 11",
"flavorRef" : "b2-7",
"networks" : "Ext-Net"
"key_name": "dev"
```

Server configuration with Scaleway : 
```sh
WIP
``` 

### III. Activate sergent on the server

Transfer the sergent archive on the server in ``/usr/local/bin/`` (with WinSCP or another file transfer tool)  
Connect to the server with ssh and execute this small script :
```sh
cd /
sudo su
chmod +x /usr/local/bin/sergent
if [ ! -f "/lib/systemd/system/sergent.service" ]; then
	echo "sergent.service creating..."
	cat > /lib/systemd/system/sergent.service << EOF
	[Unit]
	Description=Sergent Service
	After=network.target
	[Service]
	ExecStart=/bin/sh -c 'export SERGENT_COMMAND_PATH=/opt/webdrone/common; \
		/usr/local/bin/sergent \
		-Dquarkus.http.host=0.0.0.0 \
		-Dquarkus.http.port=8880 > /var/log/sergent.log 2>&1'
	Restart=always
	Type=simple
	[Install]
	WantedBy=multi-user.target
	EOF
    systemctl enable sergent.service
    echo "sergent.service installed."
fi
systemctl start sergent.service
echo "sergent.service started."
```

You can try to execute a sergent command with postman, like "list" to test the correct installation  
You can also check sergent status with the command ``systemctl status sergent.service`` or access sergent's log in ``/var/log/sergent.log``  

After everything you can ask the INFRA team to create you a new sergent-image with the instance we've created before and delete the old one.  

It's important to update **every** sergent-image version for all possible server providers to remain consistent in the sergent versions used
