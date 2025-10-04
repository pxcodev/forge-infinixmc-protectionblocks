# Protection Blocks Mod

A Minecraft Forge mod that allows players to protect their builds using special protection blocks.

## 📋 Features

- **5 Protection Tiers**: Basic, Advanced, Superior, Elite, and Master
- **Variable Protection Ranges**: From 5 to 25 blocks radius
- **Ally System**: Share your protection with other players
- **Granular Permissions**: Control what your allies can do
- **Area Visualization**: Visually display the protected area
- **Efficient Cache System**: Optimized for servers with multiple protections
- **Zone Names**: Customize the name of your protected areas
- **Welcome Messages**: Configure messages for when players enter your zone

## 🎮 Minecraft Version

- **Minecraft**: 1.20.1
- **Forge**: 47.4.8
- **Java**: 17

## 📦 Installation

1. Download the latest version of the mod from [Releases](https://github.com/pxcodev/forge-infinixmc-protectionblocks/releases)
2. Place the `.jar` file in your Minecraft `mods` folder
3. Launch Minecraft with Forge 1.20.1

## 🔧 Building

```bash
./gradlew.bat build
```

The compiled file will be located in `build/libs/`

## 🛡️ Protection Blocks

| Block | Range | Description |
|-------|-------|-------------|
| Basic | 5 blocks | Initial protection for small builds |
| Advanced | 10 blocks | For medium-sized builds |
| Superior | 15 blocks | For large builds |
| Elite | 20 blocks | For complex bases |
| Master | 25 blocks | Maximum available protection |

## 👥 Ally System

Share your protections with other players and control their permissions:

- **Build**: Allows placing and breaking blocks
- **Interact**: Allows using doors, chests, buttons, etc.
- **Manage**: Allows managing the protection

## 🔨 Development

### Project Structure

```
src/main/java/com/infinixmc/protectionblocks/
├── blocks/          # Protection blocks
├── blockentity/     # Block entities
├── client/          # Client-side code
├── config/          # Mod configuration
├── data/            # Data persistence
├── events/          # Event handlers
├── gui/             # Graphical interfaces
├── init/            # Block, item, etc. registration
├── network/         # Network packets
└── util/            # Utilities and helpers
```

## 📝 License

This project is licensed under the MIT License.

## 👨‍💻 Author

**InfinixMC** - [pxcodev](https://github.com/pxcodev)

## 🤝 Contributing

Contributions are welcome. Please:

1. Fork the project
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📮 Support

If you find any bugs or have suggestions, please open an [issue](https://github.com/pxcodev/forge-infinixmc-protectionblocks/issues).
