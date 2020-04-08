# JavaMessaging

Java messaging app (text only), client and server. A little project that I've been working on during COVID-19 quarantine. Time was spent into making the messaging app secure using RSA and AES256 encryption and also designing a nice looking, easy-to-use user interface.

## Features

 - Nice looking UI w/ subtle animations
 - AES256 end-to-end encryption
 - Not much lol

## How to run

 - Requires Java 8 (1.8.0) both client & server
	 - The specific version I used is "1.8.0_241"

Client application is located in [Client](/client)
 - Windows please use the included .exe
 - Mac users will have to run the .jar file
	 - In terminal, navigate to the folder
		 - java -jar JavaMessaging.jar
	 - Or you can run it directly, use Finder to navigate to the folder
		 - Right-click on JavaMessaging.jar
		 - Click on "Open" and agree to any popup that shows
			 - Trust me ;)

Server application is located in [src/main/java](https://github.com/jwwang2003/JavaMessaging/tree/master/src/main/java)
 - On windows, use CMD or PowerShell and navigate to the directory
	 - java Server.java
 - Same command on for Mac users

## How to connect

Once Server.java is running (make sure it says listening on port... in the terminal), there are a few ways you can connect to it
- Locally (Same device)
	- On the Client app, connect to 127.0.0.1 with port 59001
- Local network
	- On the Client app, connect to the IP address of the machine you're running the Server.java on, most likely 192.168... with port 59001
- Outside of local network
	- Some router setup is required, which you can find many tutorials online about how to setup Port Forwarding
	- What you need to know is that you need to forward the port 59001 from the local IP address of the machine running Server.java
	- To connect to server, use the public IP address of the machine running the Server with port 59001