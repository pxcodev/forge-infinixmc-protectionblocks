# Protection Blocks Mod

Un mod de Minecraft Forge que permite a los jugadores proteger sus construcciones usando bloques de protección especiales.

## 📋 Características

- **5 Niveles de Protección**: Básico, Avanzado, Superior, Elite y Maestro
- **Rangos de Protección Variables**: Desde 5 hasta 25 bloques de radio
- **Sistema de Aliados**: Comparte tu protección con otros jugadores
- **Permisos Granulares**: Controla qué pueden hacer tus aliados
- **Visualización de Área**: Muestra visualmente el área protegida
- **Sistema de Caché Eficiente**: Optimizado para servidores con múltiples protecciones
- **Nombres de Zona**: Personaliza el nombre de tus áreas protegidas
- **Mensajes de Bienvenida**: Configura mensajes para cuando los jugadores entren a tu zona

## 🎮 Versión de Minecraft

- **Minecraft**: 1.20.1
- **Forge**: 47.4.8
- **Java**: 17

## 📦 Instalación

1. Descarga la última versión del mod desde [Releases](https://github.com/pxcodev/forge-infinixmc-protectionblocks/releases)
2. Coloca el archivo `.jar` en la carpeta `mods` de tu instalación de Minecraft
3. Inicia Minecraft con Forge 1.20.1

## 🔧 Compilación

```bash
./gradlew.bat build
```

El archivo compilado se encontrará en `build/libs/`

## 🛡️ Bloques de Protección

| Bloque | Rango | Descripción |
|--------|-------|-------------|
| Básico | 5 bloques | Protección inicial para construcciones pequeñas |
| Avanzado | 10 bloques | Para construcciones medianas |
| Superior | 15 bloques | Para construcciones grandes |
| Elite | 20 bloques | Para bases complejas |
| Maestro | 25 bloques | Máxima protección disponible |

## 👥 Sistema de Aliados

Comparte tus protecciones con otros jugadores y controla sus permisos:

- **Construcción**: Permite colocar y romper bloques
- **Interacción**: Permite usar puertas, cofres, botones, etc.
- **Gestión**: Permite administrar la protección

## 🔨 Desarrollo

### Estructura del Proyecto

```
src/main/java/com/infinixmc/protectionblocks/
├── blocks/          # Bloques de protección
├── blockentity/     # Entidades de bloque
├── client/          # Código del cliente
├── config/          # Configuración del mod
├── data/            # Persistencia de datos
├── events/          # Manejadores de eventos
├── gui/             # Interfaces gráficas
├── init/            # Registro de bloques, items, etc.
├── network/         # Paquetes de red
└── util/            # Utilidades y helpers
```

## 📝 Licencia

Este proyecto está bajo licencia MIT.

## 👨‍💻 Autor

**InfinixMC** - [pxcodev](https://github.com/pxcodev)

## 🤝 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Haz fork del proyecto
2. Crea una rama para tu característica (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📮 Soporte

Si encuentras algún bug o tienes sugerencias, por favor abre un [issue](https://github.com/pxcodev/forge-infinixmc-protectionblocks/issues).
