---
layout: doc
title: "Cómo bloquear YouTube en Android (app y navegador)"
description: "Bloquea la app de YouTube y youtube.com en Android, o permítelos solo a ciertas horas. Sin root y sin cuenta de Google: qué detiene cada método y qué no."
lang: es
ref: block-youtube-android
date: 2026-11-16
---

# Cómo bloquear YouTube en Android

Puedes bloquear YouTube en Android filtrando `youtube.com` y sus subdominios con un filtro
DNS, desinstalando la app o permitiéndola solo a ciertas horas. La vía del dominio no
necesita root ni cuenta de Google, y cubre la app, Chrome y cualquier otro navegador del
teléfono a la vez.

YouTube es un solo servicio repartido en varios nombres de host. Bloquear uno no basta:

| Nombre de host | Qué lleva |
|---|---|
| `youtube.com` | El sitio en sí |
| `www.youtube.com`, `m.youtube.com` | Web de escritorio y móvil |
| `youtube.googleapis.com` | Llamadas a la API que hace la app |
| `googlevideo.com` | Los flujos de vídeo |
| `ytimg.com` | Miniaturas y recursos del reproductor |

Bloquear `youtube.com` y `*.youtube.com` detiene las cargas de página de la app y del
sitio, pero un vídeo ya cacheado en la app puede seguir reproduciéndose y las miniaturas
pueden seguir cargando.

## Bloquéala con una lista personalizada

Acutis Firewall tiene una lista personalizada que acepta dominios exactos y comodines `*.`.

1. Abre la app → **Sitios bloqueados → Lista personalizada**.
2. Añade estas líneas:

   ```
   youtube.com
   *.youtube.com
   googlevideo.com
   *.googlevideo.com
   ytimg.com
   *.ytimg.com
   ```

3. Activa el cortafuegos. Abrir YouTube fallará con "no se puede acceder al sitio" o "sin
   conexión", porque el dominio no resuelve.
4. Pon un PIN en **Ajustes → Seguridad** para que nadie edite la lista ni desactive el
   cortafuegos.

La forma con comodín `*.youtube.com` cubre el dominio y cualquier subdominio; la línea
`youtube.com` sobra, pero no molesta. Las dos están aquí para que la lista se lea como se
leería a mano.

## Permítela solo a ciertas horas

Bloquear YouTube del todo rara vez es lo que quiere un padre. Suele ser mejor una regla de
horario.

1. **Reglas de tiempo → +** y elige **Dominio**.
2. Escribe `youtube.com` (las reglas cubren el dominio y sus subdominios).
3. Elige **BLOQUEAR** y luego **Horario** para una franja nocturna como `20:00`–`07:00`, o
   **Límite diario** para 30 minutos al día.
4. Marca los días y guarda.

Una regla de horario bloquea el dominio dentro de la franja y lo deja pasar fuera; un
límite diario lo bloquea cuando el niño lo ha usado esos minutos en el día. El uso se
cuenta a partir de las consultas DNS, así que mide el tiempo con el dominio resolviendo, no
los segundos de vídeo vistos.

## Quita la app del todo

Si el teléfono no tiene cuenta de Google, desinstala la app de YouTube (**Ajustes → Apps →
YouTube → Desinstalar**). En muchos teléfonos es una app del sistema y solo se puede
desactivar: **Desactivar** en la misma pantalla basta; el icono desaparece y la app no
puede ejecutarse.

Si el teléfono sí tiene cuenta de Google, Family Link puede bloquear la app: **Family Link
→ tu hijo → Controles → Apps → YouTube → Bloquear**. Eso también impide reinstalarla desde
Play Store. Necesita cuenta de Google en el teléfono del niño, algo que algunos padres
precisamente no quieren.

## Qué sigue pasando

- **Otro resolutor DNS.** Si el niño configura **Ajustes → Red → DNS privado** con un
  nombre de host, o activa el DNS seguro de Chrome (`chrome://settings/security`), esas
  consultas van cifradas y se saltan el filtro. Desactiva los dos, teniendo en cuenta que
  en muchos teléfonos se pueden volver a activar sin el PIN.
- **Otra VPN.** Android permite una sola VPN a la vez. Otra app de VPN sustituye a Acutis
  Firewall y el filtrado se detiene.
- **Datos móviles con proxy, o un punto de acceso de otro teléfono.** El filtrado DNS se
  aplica solo en este teléfono.
- **Contenido de YouTube dentro de otras apps**: un reproductor incrustado en una app de
  noticias carga desde `youtube.com` y se filtra, pero un vídeo ya descargado antes de que
  la regla se aplicara terminará de reproducirse.

## Preguntas frecuentes

**¿Bloquear youtube.com bloquea también YouTube Music?** Bloquea `music.youtube.com`,
porque es un subdominio. La app aparte de YouTube Music usa los mismos hosts y también deja
de funcionar.

**¿Puedo bloquear solo YouTube y no el resto de Google?** Sí. La lista personalizada acepta
dominios exactos; no afecta a nada más.

**¿Hace falta root?** No. Es filtrado DNS a través del `VpnService` de Android.

**¿El niño verá una página de aviso?** No: el dominio no resuelve y el navegador muestra su
propio error de red. No hay página de bloqueo personalizada.

**¿Puedo permitir un canal concreto?** No. El filtrado trabaja con dominios, no con rutas ni
canales.

## Instálala

- [Acutis Firewall en Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall en F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
