---
layout: doc
title: "Filtro DNS en Android para control parental (sin root)"
description: "Cómo el filtrado DNS bloquea contenido adulto y sitios peligrosos en todas las apps, en qué se diferencia de la DNS privada y cuál conviene usar."
lang: es
ref: dns-filtering-android
date: 2026-11-16
---

# Filtrado DNS para control parental en Android

El filtrado DNS bloquea una página negándose a traducir su nombre a una dirección IP, así
que funciona en todas las apps del teléfono, no solo en el navegador. Se puede hacer en el
propio dispositivo con la API `VpnService` de Android — sin root, sin cuenta y sin que
ningún proveedor vea las consultas — o apuntando la DNS privada de Android a un resolutor
que filtre por ti, que cuesta menos configurar y entrega el registro de consultas a ese
operador.

## Qué es el filtrado DNS

Toda conexión empieza con una consulta DNS: "¿cuál es la IP de ejemplo.com?". La respuesta
es a lo que se conecta la app después. Si el resolutor contesta "ese dominio no existe"
(NXDOMAIN) para un sitio de la lista, la app no tiene a dónde conectarse y la petición falla
antes de descargar nada.

Como se pregunta al DNS antes de cada conexión — lo hace el navegador, el webview integrado
de una app, el SDK de publicidad —, filtrar en esa capa cubre todos a la vez. Además es
barato: una consulta por dominio, no una inspección paquete a paquete.

## Por qué cubre todas las apps

Una extensión de navegador filtra un navegador. Un servicio de accesibilidad puede leer lo
que hay en pantalla, pero necesita el permiso de Accesibilidad, que Play restringe y que
conviene desconfiar en cualquier app. Un filtro DNS basado en VPN se coloca donde el sistema
operativo pide la resolución de nombres, así que todas las apps del teléfono lo heredan.

Por eso es la opción estándar sin root en Android. El coste es que el filtrado es por
dominio: no distingue una página de otra de un mismo sitio, y no inspecciona el vídeo que ya
llega desde un dominio que aún no estaba en la lista.

## Filtro local frente a resolutor público con filtro

| | Acutis Firewall (en el dispositivo) | Cloudflare 1.1.1.1 for Families | AdGuard DNS Family | NextDNS |
|---|---|---|---|---|
| Configuración | Instalar la app y activarla | Poner el nombre de DNS privada | Poner el nombre de DNS privada | Crear perfil y poner el nombre de DNS privada |
| Dónde vive la lista | En el teléfono | En el resolutor | En el resolutor | En el resolutor |
| Quién ve cada consulta | Nadie (se responde en local) | Cloudflare | AdGuard | NextDNS |
| Dominios propios y comodines | Sí | No | No | Sí, en el panel |
| Límites diarios y horarios | Sí | No | No | No |
| Protección contra desinstalación | Sí, con el PIN | No aplica | No aplica | No aplica |
| Precio | Gratis (MIT) | Gratis | Plan gratis | Plan gratis con límite de consultas |

El resolutor familiar de Cloudflare es `1.1.1.2` y `1.0.0.2` para malware, y `1.1.1.3` y
`1.0.0.3` para malware y contenido adulto; sus nombres de DNS privada son
`security.cloudflare-dns.com` y `family.cloudflare-dns.com`. El endpoint familiar de
AdGuard es `family.adguard-dns.com` (direcciones `94.140.14.15` y `94.140.15.16`). Los dos
operadores publican qué registran; ninguno filtra por app ni se puede editar desde el
teléfono.

La diferencia práctica es quién guarda el registro de consultas. Un resolutor de red ve
todos los dominios que consulta el teléfono, incluidos los que no bloquea. Un filtro local
responde en el dispositivo y reenvía solo lo que no ha bloqueado al resolutor que ya usa tu
red, así que el router o la operadora no ven más que antes de instalar la app.

## Límites que conviene conocer antes de confiar en él

- **El DNS cifrado esquiva el filtro.** El "DNS seguro" de Chrome y la DNS privada de
  Android envían las consultas por HTTPS o TLS, fuera del puerto 53, donde un filtro local
  no las ve. Desactiva los dos en el teléfono de un niño. Con un resolutor de red esto no
  pasa: el resolutor es el que recibe la pregunta.
- **Una VPN a la vez.** Un filtro DNS local es un servicio VPN. Cualquier otra app de VPN lo
  sustituye, y Android solo muestra una VPN activa.
- **El bloqueo es por nombre.** Un sitio recién registrado, o contenido para adultos
  alojado en un dominio compartido que también sirve páginas legítimas, puede no estar
  bloqueado hasta que una lista se actualice.
- **La app ve un error de red, no una página de bloqueo.** Los dominios bloqueados
  responden NXDOMAIN, así que el navegador muestra su propio "no se puede acceder al
  sitio". No hay página propia ni mensaje por sitio.
- **Tráfico que nunca pregunta al DNS.** Las apps que llevan una IP escrita a mano o que usan
  su propio resolutor cifrado quedan fuera del filtrado DNS.

## Cómo comprobar que funciona

1. Con el filtro activo, abre un dominio de la lista. No debe cargar.
2. Desactívalo y recarga. Debe cargar: eso demuestra que quien bloqueaba era el filtro y no
   la red.
3. En Acutis Firewall, el contador de bloqueos de la pantalla principal sube según visitas
   sitios bloqueados; si se queda quieto mientras navegas a ellos, la VPN no está
   interceptando (hay otra VPN activa o la DNS privada está encendida).

## Preguntas frecuentes

**¿Hace falta root?** No. Usa el `VpnService` de Android, la misma API que una app de VPN, y
Android 10 o superior.

**¿Ralentiza el teléfono?** Un filtro local solo atiende el DNS, que es una parte pequeña del
tráfico. Acutis Firewall solo encamina por el túnel las direcciones de DNS, así que las
descargas y el vídeo no pasan por la app.

**¿Es lo mismo que un cortafuegos?** Es una clase de cortafuegos. Un filtro de paquetes
también puede bloquear puertos y direcciones; un filtro DNS solo controla la resolución de
nombres, que es lo que cubre el filtrado de contenido y los límites de tiempo.

**¿Bloquea anuncios?** Sí, si los dominios de publicidad están en una lista. No quita los
anuncios que se sirven desde el mismo dominio que el contenido.

**¿Qué es mejor para el teléfono de un niño?** Un filtro local si quieres reglas por
dominio, límites de tiempo y ningún registro fuera del teléfono. Un resolutor con filtro si
quieres configurarlo en un solo campo y te parece bien que el proveedor vea las consultas.

## Instálala

- [Acutis Firewall en Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall en F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
