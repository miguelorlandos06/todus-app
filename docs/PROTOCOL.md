# toDus protocol summary (v0.0.1)

Source: `upload/guía.txt` + smali de la APK oficial (`cu.todus.android`).

## 1. Endpoints

| Servicio | Host | Puerto | Tipo |
|---|---|---|---|
| Auth HTTP | `auth.todus.cu` | 443 | HTTP/protobuf |
| XMPP principal | `im.todus.cu` | 1756 | TLS socket |
| XMPP alternativo | `im.todus.cu` | 5443 | TLS socket |
| XMPP legacy (obsoleto) | `im.todus.cu` | 5222 | TLS socket |
| MUC Light | `muclight.im.todus.cu` | — | XMPP |
| Canales | `ch.im.todus.cu` | — | XMPP |
| Estados | `status@status.im.todus.cu` | — | XMPP |
| Bloqueos | `blocks@blocks.im.todus.cu` | — | XMPP |
| Cercanos | `near@near.im.todus.cu` | — | XMPP |
| S3 | `s3.todus.cu` | 443 | HTTP |
| Stickers | `stickers.todus.cu` | 443 | HTTP |
| Términos | `https://todus.cu/terms.html` | — | web |

## 2. Autenticación HTTP

Todo es protobuf wire-type 2 (length-delimited), construido a mano.

### Login con número de teléfono solo (path usado por v0.0.1)

```
POST /v2/auth/token
content-type: application/octet-stream
user-agent: ToDus 2.1.1

field 1 (0x0A): phone (10 dígitos cubanos, ej "5350000000")
field 2 (0x12): "fake1234567890abcdef12345678"
```

Respuesta: campo 1 = JWT.

### Flujo SMS completo (reserva si el login por teléfono se parchea)

```
POST /v2/auth/users.reserve       { 1: phone, 2: install_uuid (150 chars) }
  → SMS 6 dígitos
POST /v2/auth/users.register      { 1: phone, 2: install_uuid, 3: code }
  → campo 1 = password (96 chars hex)
POST /v2/auth/token               { 1: phone, 2: password, 3: install_uuid, 4: version_code }
  → campo 1 = JWT
```

Headers comunes: `User-Agent: ToDus 2.1.2 Auth`, `Content-Type: application/x-protobuf`.
`AUTH_VERSION_NAME = "2.1.2"`, `AUTH_VERSION_CODE = "30102"`.

## 3. XMPP

### Namespaces abreviados

| Estándar | toDus |
|---|---|
| jabber:client | jc |
| http://etherx.jabber.org/streams | x1 |
| urn:ietf:params:xml:ns:xmpp-sasl | x2 |
| urn:ietf:params:xml:ns:xmpp-bind | x4 |
| receipts / acks | x8 |
| group info | x10 |
| group members | x11 |
| group leave | x13 |
| group invite link | x14 |
| group create | x16 |
| chat states (XEP-0085) | uc1 |
| reply | reply:n |
| resend (forward) | resend:n |
| edited | edited:n |
| deleted | deleted:n |
| offline | t:offline |
| offline-del | t:offline:del |
| ping | urn:xmpp:ping |
| MAM (XEP-0313) | urn:xmpp:mam:1 |

### Atributos abreviados

| Estándar | toDus |
|---|---|
| `to=` | `o=` |
| `from=` | `f=` |
| `id=` | `i=` |
| `type=` | `t=` |
| `<presence>` | `<p>` |
| `<message>` | `<m>` |
| `<body>` | `<b>` |
| `<iq>` | `<iq>` (igual) |

### Handshake

1. Cliente → server: `<stream:stream xmlns='jc' o='im.todus.cu' xmlns:stream='x1' v='1.0'>`
2. Server → cliente: `<stream:features><es xmlns='x2'>…</es></stream:features>`
3. Cliente: `<ah xmlns='ah:ns' e='PLAIN'>base64(\0phone\0token)</ah>`
4. Server: `<ok xmlns='x2'/>`
5. Cliente (stream restart): reenvía `<stream:stream …>`
6. Cliente (bind): `<iq i='…' t='set'><b1 xmlns='x4'><re>md5(phone)_Android</re></b1></iq>`
7. Server: `<iq t='result' i='…'/>`
8. Cliente: `<p xmlns='jc'><status></status></p>`

### Restricciones del servidor

- **Solo 1 sesión por JID**: abrir una segunda conexión mata la primera. El SDK debe compartir
  el socket del listener con los sends.
- Reconexión: backoff exponencial + jitter ±20%, base 15 s, máximo 300 s.
- TLS a menudo auto-firmado; en este cliente aceptamos cualquier certificado (ver
  `XmppSession.TodusTrustManager`) pero la recomendación es fijar el certificado real.

## 4. Mensajería 1:1 (t='c')

### Texto
```xml
<m o='5350000000@im.todus.cu' t='c' i='<32hex>' xmlns='jc'>
  <k xmlns='x8'/>
  <b>texto escapado</b>
</m>
```
`<k xmlns='x8'/>` es **obligatorio**. `msg_id` = 32 hex chars.

### Reply (antes de `<b>`)
```xml
<reply xmlns='reply:n' mi='<id_original>'/>
```

### Forward (reenvío)
```xml
<resend xmlns='resend:n' i='<rid_nuevo>' mi='<id_original>' uowner='<jid_original>'/>
```
**Crítico**: `i=` es un ID nuevo, distinto del `i=` del `<m>` exterior. Si lo reutilizas, la APK
lo interpreta como reply, no forward.

### Editar
```xml
<m o='…' t='c' i='<id_original>' xmlns='jc'>
  <k xmlns='x8'/>
  <edited xmlns='edited:n' i='<edit_id_nuevo>' mi='<id_original>'/>
  <b>texto nuevo</b>
</m>
```

### Eliminar
```xml
<m o='…' t='c' i='<id>' xmlns='jc'>
  <k xmlns='x8'/>
  <deleted xmlns='deleted:n' i='<did>' mi='<id_del_borrado>'/>
  <b/>
</m>
```

### Chat states (XEP-0085 abreviado, **invertido** vs SDK viejo)
```xml
<csp xmlns='uc1'/> → composing   (csp = composing en la versión actual)
<csc xmlns='uc1'/> → paused      (csc = paused en la versión actual)
<csa xmlns='uc1'/> → active
<csi xmlns='uc1'/> → inactive
<csg xmlns='uc1'/> → gone
```

### Recibos
```xml
<dd xmlns='x8' i='<msg_id>'/>  → delivery (entregado)
<rd xmlns='x8' i='<msg_id>'/>  → read (leído)
```

### ACK
```xml
<ak xmlns='x8' i='<msg_id>' o='<destino>'/>
```

## 5. Mensajes offline (crítico)

1. Tras conectar y hacer presence: `<iq t='get'><query xmlns='t:offline'/></iq>`
2. Servidor envía los mensajes pendientes. Cada uno va precedido por
   `<todus_offline ts='<timestamp_ms>'/>`.
3. Al final del bloque: `<todus_end_offline/>`
4. Cliente confirma IDs con:
```xml
<iq t='set'>
  <query xmlns='t:offline:del'>
    <id>msg_id_1</id>
    <id>msg_id_2</id>
    ...
  </query>
</iq>
```

## 6. Archivos (S3)

### Reservar URL de subida
```xml
<iq i='<sid>-3' t='get'>
  <query xmlns='todus:purl' type='<0-5>' persistent='<true|false>' size='<bytes>' room=''/>
</iq>
```
Tipos: 0=FILE, 1=VOICE, 2=AUDIO, 3=VIDEO, 4=PICTURE, 5=PROFILE. El nombre del archivo **no va
en el IQ** — va en `Content-Disposition` del PUT HTTP.

Respuesta: `<query put='<upload_url>' get='<download_url>' status='200'/>`

### Subir
```
PUT <upload_url>
Content-Type: application/octet-stream
Content-Length: <size>
Content-Disposition: attachment; filename="<nombre>"
```

### Resolver URL real de descarga
```xml
<iq i='<sid>-2' t='get'>
  <query xmlns='todus:gurl' url='<download_url_corta>'/>
</iq>
```
Respuesta: `<query du='<url_real_firmada>' status='200'/>`

**Importante**: si `status != 200`, `du` no es válida. Si la URL contiene `X-Amz-Signature=`
(S3 pre-signed), **NO** enviar `Authorization: Bearer <token>`.

## 7. Privacidad

### Perfil
Get: `<query xmlns='todus:privacy'/>` (tipo get)
Set:
```xml
<query xmlns='todus:privacy' profile_photo='<v>' last='<v>' info='<v>'/>
```
Valores válidos: `all`, `contacts`, `exceptions`, `none` (constantes de la APK).

### Grupos
Get: `<query xmlns='todus:group:privacy'/>`
Set:
```xml
<query xmlns='todus:group:privacy' who_can='<v>' exceptions='<tel1,tel2>'/>
```

## 8. Bloqueos

```xml
Bloquear:   <query xmlns='todus:block:set' jid='<jid_completo>'/>
Desbloquear: <query xmlns='todus:block:unset:2' contact='<SOLO_TELÉFONO>'/>
Lista:       <query xmlns='todus:block:get:2'/>   → atributo blist con JIDs separados por ","
```

**Bug común**: desbloquear debe enviar solo el teléfono, no el JID completo.

## 9. Última conexión

```xml
<iq t='get' o='<jid_usuario>'><query xmlns='todus:last:2'/></iq>
```

## 10. Push (Firebase)

```xml
<iq t='set'><query xmlns='todus:fb:push'><push_id>…</push_id></query></iq>
```

## 11. Historial (MAM XEP-0313)

```xml
<iq t='set' i='<query_id>'>
  <query xmlns='urn:xmpp:mam:1' queryid='<query_id>'>
    <x xmlns='jabber:x:data' type='form'>
      <field var='FORM_TYPE' type='hidden'><value>urn:xmpp:mam:1</value></field>
      <field var='with'><value>jid</value></field>
      <field var='start'><value>fecha</value></field>
      <field var='end'><value>fecha</value></field>
    </x>
    <set xmlns='http://jabber.org/protocol/rsm'><max>N</max></set>
  </query>
</iq>
```

## 12. Parser — reglas críticas

- Aceptar `o=` o `to=`, `i=` o `id=`, `t=` o `type=` (compat legacy).
- `<b>` puede anidar; contar niveles, no regex non-greedy.
- Soportar stanzas self-closing (`<m .../>`).
- Deduplicar por `msg_id` (LRU, límite ~10000).
- Buffer incremental: procesar solo el chunk nuevo, conservar fragmento incompleto.

## 13. Riesgos de seguridad

1. **Login con número de teléfono solo**: cualquiera con el número puede autenticarse si el
   servidor no parchea el secreto débil.
2. **Inyección XML**: varios módulos del SDK Python concatenaban parámetros sin escape; este
   cliente usa `StanzaParser.escape` para todos los valores.
3. **TLS auto-firmado**: este cliente acepta cualquier certificado contra el host de toDus
   (`TodusTrustManager`). La mitigación correcta es pinning de certificados.
4. **Token = contraseña**: el JWT devuelto por `/v2/auth/token` es equivalente a una contraseña;
   no loguearlo ni compartirlo. La sesión se excluye del backup (ver `xml/backup_rules.xml`).

## 14. Errores del SDK Python (corregidos aquí)

| Módulo | Bug | Fix aquí |
|---|---|---|
| `unblock_user` | enviaba JID completo | `StanzaBuilder.unblockUser(phone)` envía solo el teléfono |
| `privacy` | defaults `everyone`/`nobody` | se usan `all`/`none`/`contacts`/`exceptions` |
| `parse_iq` | no extraía `status` de `GetUrlIQ` | se valida `status == 200` |
| `parse_iq` | no extraía `blist` de block list | se parsea y emite `BlockedContacts` |
| offline | no implementado | `getOffline` + `ackOffline` + eventos `OfflineComplete` |
| `upload_query` | XML malformado (`room=''{n_attr}>`) | `reserveUploadUrl` genera XML bienformado |
| chat states | `csp`=`paused` en SDK viejo | `csp`=`composing` (versión actual de la APK) |
