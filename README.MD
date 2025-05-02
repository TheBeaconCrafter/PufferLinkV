# PufferLinkV (Velocity)

> **Note:** This is a port of [PufferLink](https://github.com/TheBeaconCrafter/PufferLink) to Velocity. It is not finished and code-wise by far not the best implementation of a port, but it works perfectly fine.

A BungeeCord plugin for managing PufferPanel v3 servers directly from your Minecraft server. This plugin allows server administrators to control and monitor their PufferPanel servers through in-game commands.

> **Note:** This project is not affiliated with PufferPanel. For the official PufferPanel project, visit [PufferPanel GitHub](https://github.com/pufferpanel/pufferpanel)

## Features

- 🔄 Server Management
  - Start/Stop/Restart servers
  - Execute console commands remotely
  - View server status
  - List all available servers
- 📊 Server Monitoring
  - Automatic server status monitoring
  - Real-time online/offline status updates
- 🎨 Customizable Prefix
- 🔐 Secure Authentication
  - Session-based API authentication
  - Permission-based access control

## Configuration

Create or modify `config.yml`:

```yaml
api-url: "" # Example: https://panel.example.org
email: ""
password: ""
prefix: "&3Cloud &8» &r" # Supports color codes with &
enable-monitor: true # Enable server online/offline monitor
```

## Security Recommendations

It is strongly recommended to:
1. Create a dedicated PufferPanel account for use with this plugin
2. Grant permissions only to servers you want to manage in-game
3. Use an account with minimal required permissions
4. Keep your credentials secure and never share them

## Commands

- `/cloud list` - List all available servers
- `/cloud up` - List all online servers
- `/cloud status <id>` - View server status
- `/cloud start <id>` - Start a server
- `/cloud stop <id>` - Stop a server
- `/cloud restart <id>` - Restart a server
- `/cloud console <id> <command>` - Send console command to server

## Permissions

- `pufferlink.use` - Base permission for using the plugin
  - Allows use of `/cloud` command and aliases
  - Required for all plugin functionality
- `pufferlink.notify` - Users with this permission will receive monitoring updates

## Command Aliases

- `/cloud`
- `/puffer`

## Installation

1. Place the plugin JAR in your BungeeCord's `plugins` folder
2. Start/restart your BungeeCord server
3. Configure the `config.yml` with your PufferPanel credentials
4. Restart BungeeCord or reload the plugin

## Support

For issues, bugs, or feature requests, please create an issue in the project's repository.
