---
layout: doc
title: "Alternativa a Family Link para Android (gratis)"
description: "Family Link necesita una cuenta de Google en el teléfono del niño y no filtra dentro de todas las apps. Qué hace mejor una alternativa gratuita en el propio móvil."
lang: es
ref: family-link-alternative
date: 2026-11-16
---

# Una alternativa gratis a Google Family Link para Android

Si quieres control parental en un teléfono Android sin cuenta de Google, Family Link no es
una opción: gestiona una cuenta de Google, así que el niño necesita una y el padre otra. La
alternativa es un filtro DNS en el propio dispositivo, gratis: hace filtrado de contenido,
límites de tiempo y protección contra desinstalación desde el teléfono, sin cuenta en
ninguno de los dos lados.

## Qué hace bien Family Link

Family Link es gratis, lo hace Google y cubre cosas que un filtro en el dispositivo no
puede:

- **Gestión de apps.** Aprueba o bloquea instalaciones de Play Store, y bloquea apps ya
  instaladas, incluidas las del sistema como YouTube.
- **Tiempo de pantalla.** Límites diarios y una hora de dormir que bloquea el dispositivo
  entero, no un dominio.
- **Ubicación.** La ubicación del teléfono del niño en la app de padres.
- **Control por cuenta.** El filtrado sigue a la cuenta de Google, así que Chrome con esa
  sesión en cualquier dispositivo se comporta igual.
- **Administración remota.** Todo se cambia desde el teléfono del padre.

Google lo documenta en [families.google/familylink](https://families.google/familylink/).

## Dónde no encaja Family Link

- **El teléfono no tiene cuenta de Google.** Un primer teléfono, uno heredado, una ROM sin
  servicios de Google o un dispositivo donde decidiste no poner la cuenta. Family Link no
  puede gestionar lo que no existe.
- **No quieres una cuenta de Google para un menor.** Crear una supone que Google guarda la
  identidad, el historial de ubicaciones y la lista de apps.
- **Quieres reglas por dominio.** Family Link bloquea apps y resultados de búsqueda de
  Google. No tiene lista de dominios, ni comodines, ni "este sitio solo de 18:00 a 20:00".
- **Quieres que el filtro siga funcionando sin conexión.** Un filtro local responde en el
  teléfono; las restricciones de Family Link se consultan contra los servicios de Google.

## Qué hace en su lugar un filtro en el dispositivo

Acutis Firewall levanta una VPN local que filtra el DNS, y es gratis con licencia MIT:

- **Listas** de contenido adulto, malware y phishing, apuestas y redes sociales, más una
  lista personalizada que acepta dominios exactos y comodines `*.`.
- **Reglas de tiempo** por dominio: un horario (bloquear YouTube de 20:00 a 07:00) o un
  límite diario (30 minutos de YouTube).
- **Un PIN** que protege los ajustes, y protección contra desinstalación opcional que
  bloquea desinstalar, forzar la detención y borrar los datos. Android sigue permitiendo
  desactivar un administrador del dispositivo en Ajustes, y la app te avisa cuando ocurre.
- **Sin cuenta, sin servidor y sin analítica.** Las consultas se responden en el teléfono y
  las listas se descargan de listas públicas de código abierto.

## Una al lado de la otra

| | Family Link | Acutis Firewall |
|---|---|---|
| Cuenta de Google en el teléfono del niño | Necesaria | No hace falta |
| Bloquea contenido adulto en todas las apps | En parte, vía servicios de Google | Sí, en la capa DNS |
| Bloquea una app concreta | Sí | Solo bloqueando sus dominios |
| Tiempo de pantalla del dispositivo completo | Sí | No: solo por dominio |
| Reglas por dominio y comodines | No | Sí |
| Ubicación | Sí | No |
| Administración remota | Sí | No: se configura en el teléfono |
| Quién ve las consultas | Google | Nadie |
| Precio | Gratis | Gratis (MIT) |

Se complementan: Family Link para instalaciones de apps y tiempo de pantalla del dispositivo
completo, un filtro DNS para el control por dominio y para el caso en que directamente no
hay cuenta.

## ¿Puedo usar las dos?

Sí, y no chocan, pero solo puede haber una VPN activa a la vez. Acutis Firewall es un
servicio VPN, así que si el niño instala otra app de VPN, el filtrado se detiene hasta que
la quite. Family Link no usa VPN, así que sigue funcionando al mismo tiempo.

Dos detalles prácticos: añade Acutis Firewall a las apps aprobadas de Family Link para que no
se bloquee por error, y pon un PIN antes de entregar el teléfono.

## Preguntas frecuentes

**¿De verdad es gratis?** Sí. Acutis Firewall tiene licencia MIT, sin anuncios, sin
suscripción y sin compras dentro de la app. Las donaciones son opcionales.

**¿Hay que hacer root?** No. El filtrado usa la API `VpnService` de Android.

**¿Sustituye a Family Link por completo?** No para el tiempo de pantalla del dispositivo
completo ni para aprobar apps, y no tiene funciones de ubicación. Sustituye el filtrado de
contenido y añade reglas por dominio.

**¿Funciona en un teléfono sin cuenta de Google?** Sí. Es el caso para el que se hizo: nada
en la app necesita una cuenta ni un servicio de Google.

**¿Qué ve el niño?** Un error de red cuando un dominio está bloqueado, y una notificación
mientras el cortafuegos está activo. La app no tiene página de bloqueo.

## Instálala

- [Acutis Firewall en Google Play](https://play.google.com/store/apps/details?id=com.acutis.firewall)
- [Acutis Firewall en F-Droid](https://f-droid.org/packages/com.acutis.firewall/)
