---
layout: doc
title: "Cómo bloquear contenido adulto en Android (gratis)"
description: "Tres formas de bloquear pornografía y páginas para adultos en Android, qué se le escapa a cada una y cómo montar un filtro gratis en el teléfono en cinco minutos."
lang: es
ref: block-adult-content-android
date: 2026-11-16
---

# Cómo bloquear contenido adulto en Android

Puedes bloquear páginas para adultos en Android sin root y en unos cinco minutos con
Google Family Link, con una DNS privada que filtre o con un filtro DNS en el propio
teléfono, como Acutis Firewall. Family Link es lo más rápido si el niño ya tiene cuenta de
Google; el filtro en el dispositivo es la única opción que funciona en un teléfono sin
cuenta de Google y sin que ningún proveedor vea las consultas.

| Método | ¿Hace falta cuenta? | Cubre todas las apps | Quién ve las consultas DNS |
|---|---|---|---|
| Google Family Link | Sí, en los dos teléfonos | Sí | Google |
| DNS privada con filtro | No | Sí, hasta que el navegador usa su propio DoH | El operador del resolutor |
| Acutis Firewall | No | Sí, salvo el tráfico que esquiva el DNS del sistema | Nadie: se responde en el teléfono |

## Método 1: Google Family Link

Family Link filtra controlando la cuenta de Google del niño, no el tráfico del teléfono.

1. Instala **Google Family Link para padres** en tu teléfono y **Family Link** en el del
   niño, con la sesión de la cuenta del niño.
2. Sigue el proceso de vinculación hasta que las dos apps muestren la misma familia.
3. En la app de padres, abre al niño y entra en **Controles → Restricciones de
   contenido → Búsqueda de Google**; activa **Filtrar resultados explícitos**.
4. En **Controles → Apps**, limita las instalaciones para que tengas que aprobarlas. Así
   no puede cambiar un navegador filtrado por otro sin filtro.

Qué se le escapa: Family Link restringe los servicios de Google y las instalaciones de
apps. No inspecciona el DNS, así que una página abierta en un navegador que no gestiona,
o una app ya instalada fuera de Play Store, no queda filtrada.

## Método 2: DNS privada

Android 9 y posteriores pueden enviar todo el DNS a un resolutor cifrado que tú eliges.
Varios resolutores tienen un endpoint familiar que descarta los dominios para adultos y
de malware antes de resolverlos:

- **Cloudflare 1.1.1.1 for Families**: nombre de DNS privada `family.cloudflare-dns.com`
  (o las direcciones `1.1.1.3` y `1.0.0.3`).
- **AdGuard Family**: nombre de DNS privada `family.adguard-dns.com`.

Se configura en **Ajustes → Red e internet → DNS privado → Nombre de host del proveedor
de DNS privado**. Android lo mantiene en todas las redes a las que se conecte el teléfono,
incluidos los datos móviles.

Qué se le escapa: las consultas van a ese proveedor, que puede registrarlas, y no puedes
añadir dominios propios ni reglas por app. Un navegador con su propio DNS over HTTPS
(`chrome://settings/security → Usar DNS seguro`) puede saltarse la DNS privada por
completo.

## Método 3: un filtro DNS en el propio teléfono

Acutis Firewall levanta una VPN local que responde al DNS en el teléfono. Las consultas de
un dominio de sus listas no salen del dispositivo; el resto se reenvía al resolutor que ya
usa tu red.

1. Instala Acutis Firewall desde
   [Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall) o
   [F-Droid](https://f-droid.org/packages/com.acutis.firewall/).
2. Ábrela y activa el cortafuegos. Android pedirá permiso para configurar una conexión VPN:
   ese permiso es lo que permite a la app ver las consultas DNS. Acéptalo.
3. Entra en **Sitios bloqueados** y comprueba que **Contenido adulto** está activado. Viene
   activado por defecto; el contador bajo el interruptor muestra cuántos dominios tiene la
   lista.
4. Pulsa **Actualizar listas** para tenerla al día. Se descarga de listas públicas de
   código abierto, no de un servidor del desarrollador.
5. Pon un PIN en **Ajustes → Seguridad → Activar protección con PIN**, para que el
   cortafuegos no se pueda desactivar desde el teléfono.

Qué se le escapa: el filtro solo ve el DNS del puerto 53. El DNS seguro de Chrome y la DNS
privada de Android cifran las consultas y se saltan el túnel: desactiva los dos. Cualquier
otra VPN activa también sustituye a Acutis Firewall, porque Android permite una sola VPN a
la vez. Y un filtro DNS bloquea por nombre de dominio: una página nueva, o contenido para
adultos servido desde un dominio compartido, puede resolverse antes de que la lista se
actualice.

## Cuál elegir

- **El niño tiene cuenta de Google y quieres el mínimo esfuerzo**: Family Link, y DNS
  privada para tapar los huecos.
- **El teléfono no tiene cuenta de Google**: DNS privada si aceptas que el operador del
  resolutor vea las consultas; Acutis Firewall si no.
- **Quieres reglas por dominio, límites de tiempo y PIN**: Acutis Firewall, que es gratis,
  de código abierto y no necesita cuenta.

Family Link y Acutis Firewall pueden convivir, porque hacen cosas distintas, pero no pongas
dos filtros basados en VPN a la vez.

## Preguntas frecuentes

**¿Hace falta root?** No. El filtrado DNS usa la API `VpnService` de Android, que
cualquier app puede solicitar sin root.

**¿Bloquea también dentro de las apps, no solo en el navegador?** Sí, en cualquier app que
resuelva dominios a través del DNS de Android. La excepción son las apps con servidores DNS
propios o con su propio DNS cifrado.

**¿Mi hijo puede desactivarlo?** Con Acutis Firewall, solo con el PIN: la protección contra
desinstalación bloquea desinstalar, forzar la detención y borrar los datos. Un usuario
decidido siempre puede desactivar el administrador del dispositivo en Ajustes, y la app te
avisa cuando ocurre. Lo cuentas en [protección contra
desinstalación](proteccion-desinstalacion.html).

**¿Es gratis?** Acutis Firewall tiene licencia MIT y es gratis, sin cuenta, anuncios ni
suscripción. Family Link es gratis pero necesita cuenta de Google. Los resolutores
familiares de Cloudflare y AdGuard también son gratis.

## Instálala

- [Acutis Firewall en Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall en F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
