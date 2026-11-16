---
layout: doc
title: "¿Olvidaste el PIN de control parental? Cómo quitarlo"
description: "El PIN no se puede recuperar. Cómo quitarlo según cómo esté configurado — solo PIN, PIN con protección de desinstalación o Device Owner — sin perder el teléfono."
lang: es
ref: forgot-pin
---

# ¿Olvidaste el PIN?

El PIN no se puede recuperar. Nunca sale del teléfono y la app solo guarda una forma
cifrada que no se puede volver a convertir en el PIN, así que nadie puede consultarlo, ni
siquiera nosotros. Lo que sí puedes hacer es quitarlo, y cómo depende de cómo esté
configurada la app.

**¿Qué configuración tienes?** Abre **Ajustes** de Android y busca **administrador del
dispositivo**.

- Acutis Firewall no aparece ahí, o está desactivado: [solo PIN](#solo-pin).
- Acutis Firewall aparece activado: [PIN y protección de desinstalación](#pin-y-proteccion-de-desinstalacion).
- Configuraste el teléfono con el comando `adb shell dpm set-device-owner`: [Device Owner](#device-owner).

## Solo PIN

El PIN solo protege los ajustes de la propia app, así que Android te deja hacer cualquiera
de estas dos cosas:

- **Desinstalar la app** como cualquier otra, y volver a instalarla si quieres empezar de
  cero.
- **Borrar sus datos**: Ajustes → Aplicaciones → Acutis Firewall → Almacenamiento y caché →
  Borrar datos. Eso elimina el PIN junto con todo lo demás que guarda la app, incluidos tus
  horarios y las listas. Después abre la app, pon un PIN nuevo y vuelve a activar el
  cortafuegos.

## PIN y protección de desinstalación

Android se niega a desinstalar la app y bloquea el botón de borrar datos hasta que se quita
la protección de desinstalación. Sin el PIN no puedes quitarla desde dentro de la app, pero
Android siempre deja quitarla desde sus propios Ajustes:

1. Abre **Ajustes** de Android y busca **administrador del dispositivo**. Normalmente está
   en Seguridad → Aplicaciones de administración del dispositivo.
2. Toca **Acutis Firewall**, luego **Desactivar esta app de administración** y confirma. Si
   aparece una pantalla pidiendo el PIN, no puede impedirlo: toca Cancelar, vuelve a Ajustes
   y confirma.
3. Acutis Firewall mostrará **Se quitó la protección contra desinstalación**. Es lo esperado:
   es el aviso que dispararía un niño, y la app no puede distinguir a los dos.

Después, o bien:

- **Desinstala la app** como siempre. Eso también quita el aviso.
- **Consérvala, con un PIN nuevo**: borra sus datos (Ajustes → Aplicaciones → Acutis
  Firewall → Almacenamiento y caché → Borrar datos), abre la app, pon un PIN nuevo y vuelve
  a activar la protección contra desinstalación. El aviso seguirá apareciendo hasta que la
  protección esté activa otra vez. Borrar los datos no puede ocultarlo, porque un niño podría
  hacer lo mismo.

## Device Owner

Si configuraste el teléfono con `adb shell dpm set-device-owner`, Android no deja que nada
salvo la propia app lo quite, y desactivarlo dentro de la app pide el PIN. Sin el PIN, **la
única salida es un restablecimiento de fábrica desde el modo recovery, que borra todo el
contenido del teléfono.** La app bloquea el restablecimiento en Ajustes de Android, por eso
hay que hacerlo desde recovery.

1. Haz una copia de lo que puedas: fotos, contactos y cualquier cosa que no esté ya
   sincronizada.
2. Apaga el teléfono y arráncalo en modo recovery. Los botones dependen del modelo; suelen
   ser Encendido y Bajar volumen a la vez. Busca tu modelo y "modo recovery".
3. Elige **Restablecer datos de fábrica**.

Después del restablecimiento, Android puede pedir la cuenta de Google que había en el
teléfono. Eso es la Protección anti-restablecimiento de Android, no Acutis Firewall.

---

**La próxima vez:** apunta el PIN y guárdalo donde tu hijo no pueda alcanzarlo.
