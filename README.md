# httpproxyserver
A simple HTTP proxy server written in Kotlin for Android.

One of the possible usage scenarios:
You have a mobile device and you want to use it as a proxy server for your desktop computer that can't be used to access the Internet.
1. Install the app on your mobile device.
2. Start the app and configure the proxy server to listen on a port of your choice.
3. Forward a port on your desktop computer to the port on your mobile device. For example:
`adb forward tcp:1081 tcp:1081`
4. Configure your desktop computer to use the proxy server listening on the port on desktop computer.

