# TrollishlyIncompatible - Spigot plugin (requires PacketEvents)
Trick (non-whitelisted) users into thinking they are using the wrong Minecraft version!
And then, when they switch to the so-called ‘correct’ one, the server will claim it is running a different version: the cycle repeats itself over and over again.
## Showcase
https://github.com/user-attachments/assets/cc1377b0-f708-4d99-8024-e2f15bbc3835
- The versions it cycles through are between 1.7.2 and the latest version supported by PacketEvents.
- The version shown to the player will stay until they use the so-called ‘correct’ one.
## Limitation for ViaVersion-powered servers
- If the random version lands on the version the server is actually running on, it won’t say it is incompatible in server list.
- Example: if it lands on 1.12.2 and your server is 1.12.2, it will not specify it is incompatible in server list. But this is purely visual afaik
