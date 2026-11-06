"""HU-080: recorre el flujo completo de atención contra los 5 microservicios, locales o en la nube.

Variables: VETCARE_ADMIN_CORREO y VETCARE_ADMIN_CONTRASENA (obligatorias) y VETCARE_URL_PACIENTES,
VETCARE_URL_AGENDA, VETCARE_URL_HISTORIA, VETCARE_URL_FARMACIA y VETCARE_URL_FACTURACION.
Crea datos de prueba identificados con el prefijo Flujo. No imprime credenciales ni tokens.
"""
import datetime as dt
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.request

PAC = os.environ.get('VETCARE_URL_PACIENTES', 'http://localhost:8081').rstrip('/')
AGE = os.environ.get('VETCARE_URL_AGENDA', 'http://localhost:8082').rstrip('/')
HIS = os.environ.get('VETCARE_URL_HISTORIA', 'http://localhost:8083').rstrip('/')
FAR = os.environ.get('VETCARE_URL_FARMACIA', 'http://localhost:8084').rstrip('/')
FAC = os.environ.get('VETCARE_URL_FACTURACION', 'http://localhost:8085').rstrip('/')
ESPERA_SEGUNDOS = 180
fallos = []


def llamar(metodo, url, cuerpo=None, token=None):
    cabeceras = {'Content-Type': 'application/json'}
    if token:
        cabeceras['Authorization'] = 'Bearer ' + token
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    peticion = urllib.request.Request(url, method=metodo, data=datos, headers=cabeceras)
    try:
        with urllib.request.urlopen(peticion, timeout=120) as respuesta:
            contenido = respuesta.read()
            return respuesta.status, _json(contenido)
    except urllib.error.HTTPError as error:
        return error.code, _json(error.read())


def _json(contenido):
    try:
        return json.loads(contenido) if contenido else None
    except ValueError:
        return None


def ver(nombre, obtenido, esperado):
    correcto = obtenido == esperado
    print(('OK   ' if correcto else 'FALLA'), nombre, '->', obtenido, '' if correcto else f'(esperado {esperado})')
    if not correcto:
        fallos.append(nombre)


def esperar(servicio, url):
    limite = time.time() + ESPERA_SEGUNDOS
    while time.time() < limite:
        try:
            estado, cuerpo = llamar('GET', url + '/actuator/health')
            if estado == 200 and cuerpo.get('status') == 'UP':
                print('OK    salud', servicio)
                return
        except OSError:
            pass
        time.sleep(5)
    ver('salud ' + servicio, 'sin respuesta', 'UP')
    raise SystemExit(f'RESULTADO: {servicio} no responde')


def principal():
    try:
        correo_admin = os.environ['VETCARE_ADMIN_CORREO']
        contrasena_admin = os.environ['VETCARE_ADMIN_CONTRASENA']
    except KeyError as falta:
        raise SystemExit(f'Falta la variable de entorno {falta}')

    for servicio, url in (('ms-pacientes', PAC), ('ms-agenda', AGE), ('ms-historia-clinica', HIS), ('ms-farmacia', FAR), ('ms-facturacion', FAC)):
        esperar(servicio, url)

    sufijo = secrets.token_hex(4)
    clave = secrets.token_urlsafe(18)
    hoy = dt.date.today()
    lejos = str(hoy + dt.timedelta(days=300))
    pronto = str(hoy + dt.timedelta(days=30))

    estado, sesion = llamar('POST', PAC + '/api/auth/login', {'correo': correo_admin, 'contrasena': contrasena_admin})
    ver('Inicio de sesión del administrador', estado, 200)
    if estado != 200:
        raise SystemExit('RESULTADO: no se pudo iniciar sesión')
    admin = sesion['token']

    def usuario(rol, **extra):
        correo = f'flujo.{rol.lower()}.{sufijo}@vetcare.test'
        estado, creado = llamar('POST', PAC + '/api/usuarios', dict(nombre='Flujo ' + rol, correo=correo, contrasena=clave, rol=rol, **extra), admin)
        ver('Crear usuario ' + rol, estado, 201)
        return creado['id'], llamar('POST', PAC + '/api/auth/login', {'correo': correo, 'contrasena': clave})[1]['token']

    _, recepcion = usuario('RECEPCIONISTA')
    vet_id, vet = usuario('VETERINARIO')
    _, auxiliar = usuario('AUXILIAR_VETERINARIO')
    _, laboratorio = usuario('LABORATORIO')
    _, farmacia = usuario('FARMACIA')
    _, cajero = usuario('CAJERO')
    _, peluqueria = usuario('PELUQUERIA')

    # 1. Propietario, mascota y portal
    estado, propietario = llamar('POST', PAC + '/api/propietarios', dict(documento='F' + sufijo, nombre='Flujo Propietario', telefono='3000000000', correo=f'flujo.{sufijo}@vetcare.test', direccion='Calle Flujo'), recepcion)
    ver('HU-011 Registrar propietario', estado, 201)
    estado, mascota = llamar('POST', PAC + '/api/mascotas', dict(nombre='Flujo Max', especie='Flujo Canino', raza='Criollo', sexo='MACHO', fechaNacimiento='2022-03-15', pesoKg=12.5, propietarioId=propietario['id']), recepcion)
    ver('HU-012 Registrar mascota', estado, 201)
    m1 = mascota['id']
    _, dueno = usuario('PROPIETARIO', propietarioId=propietario['id'])

    # 2. Cita, respetando RN-03
    dias = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
    ver('HU-021 Configurar horario', llamar('PUT', f'{AGE}/api/horarios/{vet_id}', dict(nombre='Flujo Dra.', dias=dias, horaInicio='08:00', horaFin='12:00', cuposDiarios=2), admin)[0], 200)
    cita = dict(mascotaId=m1, veterinarioId=vet_id, fecha=str(hoy), hora='08:00', motivo='Flujo')
    estado, creada = llamar('POST', AGE + '/api/citas', cita, recepcion)
    ver('HU-022 Agendar cita', estado, 201)
    c1 = creada['id']
    ver('RN-03 Franja ocupada', llamar('POST', AGE + '/api/citas', cita, recepcion)[0], 409)
    ver('HU-024 Cita desde el portal', llamar('POST', AGE + '/api/portal/citas', dict(cita, hora='08:30'), dueno)[0], 201)
    ver('RN-03 Sin cupos', llamar('POST', AGE + '/api/citas', dict(cita, hora='09:00'), recepcion)[0], 409)
    ver('HU-071 Próximas citas en el portal', len(llamar('GET', AGE + '/api/portal/citas', token=dueno)[1]), 2)
    ver('HU-026 Registrar llegada', llamar('PATCH', f'{AGE}/api/citas/{c1}/llegada', token=recepcion)[1]['estado'], 'EN_ESPERA')

    # 3. Consulta
    ver('HU-032 Signos vitales', llamar('PUT', f'{HIS}/api/citas/{c1}/signos-vitales', dict(pesoKg=12.5, temperatura=38.6, frecuenciaCardiaca=90, frecuenciaRespiratoria=24), auxiliar)[0], 200)
    estado, consulta = llamar('POST', HIS + '/api/consultas', {'citaId': c1}, vet)
    ver('HU-031 Abrir consulta', estado, 201)
    cid = consulta['id']
    ver('HU-033 Registro clínico', llamar('PUT', f'{HIS}/api/consultas/{cid}/registro-clinico', dict(anamnesis='Flujo', examenFisico='Flujo', diagnostico='Flujo', planTratamiento='Flujo'), vet)[0], 200)

    # 4. Laboratorio
    estado, orden = llamar('POST', f'{HIS}/api/consultas/{cid}/ordenes-laboratorio', {'examen': 'Flujo Hemograma'}, vet)
    ver('HU-041 Orden de laboratorio', estado, 201)
    ver('HU-043 Resultado', llamar('PUT', f"{HIS}/api/ordenes-laboratorio/{orden['id']}/resultado", {'resultado': 'Normal'}, laboratorio)[1]['estado'], 'COMPLETADA')

    # 5. Fórmula y despacho, con RN-02 y RN-04
    estado, medicamento = llamar('POST', FAR + '/api/medicamentos', dict(codigo='F-' + sufijo, nombre='Flujo Metronidazol', tipo='MEDICAMENTO', presentacion='Caja', precio=1500, stockMinimo=2), farmacia)
    ver('HU-051 Registrar medicamento', estado, 201)
    med = medicamento['id']
    llamar('POST', f'{FAR}/api/medicamentos/{med}/lotes', dict(numero='B-' + sufijo, vencimiento=lejos, cantidad=20), farmacia)
    ver('HU-052 Registrar lote', llamar('POST', f'{FAR}/api/medicamentos/{med}/lotes', dict(numero='A-' + sufijo, vencimiento=pronto, cantidad=4), farmacia)[0], 201)
    estado, formula = llamar('POST', HIS + '/api/formulas', dict(consultaId=cid, vigenteHasta=str(hoy + dt.timedelta(days=15)), items=[dict(medicamentoId=med, dosis='1', frecuencia='12 h', duracion='7 días', cantidad=14)]), vet)
    ver('HU-045 Emitir fórmula', estado, 201)
    fid = formula['id']
    despacho = lambda cantidad: llamar('POST', FAR + '/api/despachos', dict(formulaId=fid, items=[dict(medicamentoId=med, cantidad=cantidad)]), farmacia)
    ver('RN-02 No despachar más de lo formulado', despacho(20)[0], 422)
    ver('HU-053 Despachar', despacho(14)[0], 201)
    ver('RN-02 Fórmula despachada', llamar('GET', f'{HIS}/api/formulas/{fid}/vigencia', token=farmacia)[1]['estado'], 'DESPACHADA')
    kardex = llamar('GET', f'{FAR}/api/medicamentos/{med}/kardex', token=farmacia)[1]['contenido']
    salidas = [(m['loteNumero'], m['cantidad']) for m in kardex if m['tipo'] == 'DESPACHO']
    ver('RN-04 FEFO', salidas, [('A-' + sufijo, -4), ('B-' + sufijo, -10)])
    ver('RN-04 Stock final', llamar('GET', f'{FAR}/api/medicamentos/{med}', token=farmacia)[1]['stockDisponible'], 10)

    # 6. Vacunación, con RN-01 y descuento del lote
    llamar('POST', HIS + '/api/esquemas-vacunacion', dict(especie='Flujo Canino', vacuna='Rabia', dosis=2, intervaloDias=365), vet)
    vacuna = llamar('POST', FAR + '/api/medicamentos', dict(codigo='V-' + sufijo, nombre='Flujo Vacuna', tipo='MEDICAMENTO', presentacion='Dosis', precio=20000, stockMinimo=0), farmacia)[1]['id']
    llamar('POST', f'{FAR}/api/medicamentos/{vacuna}/lotes', dict(numero='V-' + sufijo, vencimiento=lejos, cantidad=3), farmacia)
    estado, dosis = llamar('POST', HIS + '/api/vacunas', dict(mascotaId=m1, vacuna='Rabia', lote='V-' + sufijo, fecha=str(hoy)), vet)
    ver('HU-037 Aplicar vacuna', estado, 201)
    ver('RN-01 Próxima dosis', dosis['proximaDosis'], str(hoy + dt.timedelta(days=365)))
    ver('HU-037 Descuenta el lote', llamar('GET', f'{FAR}/api/medicamentos/{vacuna}', token=farmacia)[1]['stockDisponible'], 2)
    ver('HU-071 Vacunas pendientes en el portal', len(llamar('GET', HIS + '/api/portal/vacunas/pendientes', token=dueno)[1]), 1)
    ver('HU-071 Mascotas en el portal', len(llamar('GET', PAC + '/api/portal/mascotas', token=dueno)[1]), 1)

    # 7. Hospitalización
    estado, hospital = llamar('POST', HIS + '/api/hospitalizaciones', dict(mascotaId=m1, motivo='Flujo', cubiculo='F-' + sufijo), vet)
    ver('HU-047 Hospitalizar', estado, 201)
    ver('HU-048 Evolución', llamar('POST', f"{HIS}/api/hospitalizaciones/{hospital['id']}/evoluciones", dict(observaciones='Flujo', medicamentosAdministrados=[med]), auxiliar)[0], 201)
    ver('HU-049 Alta', llamar('PATCH', f"{HIS}/api/hospitalizaciones/{hospital['id']}/alta", {'indicaciones': 'Flujo'}, vet)[1]['estado'], 'FINALIZADA')
    ver('HU-035 Cerrar consulta', llamar('PATCH', f'{HIS}/api/consultas/{cid}/cierre', token=vet)[1]['estado'], 'CERRADA')
    ver('HU-035 Cita atendida', llamar('GET', f'{AGE}/api/citas/{c1}', token=recepcion)[1]['estado'], 'ATENDIDA')

    # 8. Peluquería
    llamar('PUT', AGE + '/api/peluqueria/horario', dict(nombre='Flujo Peluquería', dias=dias, horaInicio='08:00', horaFin='18:00', cuposDiarios=200), admin)
    hora_peluqueria = f'{8 + secrets.randbelow(10):02d}:{secrets.choice(["00", "30"])}'
    estado, bano = llamar('POST', AGE + '/api/peluqueria/citas', dict(mascotaId=m1, fecha=str(hoy), hora=hora_peluqueria, servicio='Flujo Baño'), peluqueria)
    ver('HU-028 Agendar peluquería', estado, 201)
    ver('HU-029 Registrar servicio', llamar('PATCH', f"{AGE}/api/peluqueria/citas/{bano['id']}/servicio", {'observaciones': 'Flujo'}, peluqueria)[0], 200)

    # 9. Facturación
    tarifas = {t['servicio']: t for t in llamar('GET', FAC + '/api/tarifas', token=admin)[1]}
    for servicio, precio in (('CONSULTA', 60000), ('LABORATORIO', 30000), ('HOSPITALIZACION', 50000), ('PELUQUERIA', 39000)):
        if servicio not in tarifas:
            ver('HU-062 Tarifa ' + servicio, llamar('POST', FAC + '/api/tarifas', dict(servicio=servicio, nombre='Tarifa ' + servicio, precio=precio), admin)[0], 201)
    estado, factura = llamar('POST', FAC + '/api/facturas', {'mascotaId': m1}, cajero)
    ver('HU-061 Generar factura', estado, 201)
    ver('HU-061 Consolida los 5 servicios', sorted({i['tipo'] for i in factura['items']}), ['CONSULTA', 'HOSPITALIZACION', 'LABORATORIO', 'MEDICAMENTOS', 'PELUQUERIA'])
    ver('HU-061 Sin cobro doble', llamar('POST', FAC + '/api/facturas', {'mascotaId': m1}, cajero)[0], 422)
    fac = factura['id']
    descontada = llamar('PATCH', f'{FAC}/api/facturas/{fac}/ajustes', {'descuentoPorcentaje': 10}, cajero)[1]
    ver('HU-063 Total con descuento e impuesto', descontada['total'], descontada['subtotal'] - descontada['descuento'] + descontada['impuesto'])
    estado, caja = llamar('POST', FAC + '/api/cajas/apertura', {'base': 100000}, cajero)
    ver('HU-066 Abrir caja', estado, 201)
    total = descontada['total']
    pagada = llamar('POST', f'{FAC}/api/facturas/{fac}/pagos', {'pagos': [dict(medio='EFECTIVO', valor=total - 1000), dict(medio='TARJETA', valor=1000)]}, cajero)[1]
    ver('HU-064 Pago combinado', (pagada['saldo'], pagada['estado']), (0, 'PAGADA'))
    ver('HU-067 Factura en el portal', len(llamar('GET', FAC + '/api/portal/facturas', token=dueno)[1]), 1)
    cierre = llamar('PATCH', f"{FAC}/api/cajas/{caja['id']}/cierre", {'efectivoContado': 100000 + total - 1000}, cajero)[1]
    ver('HU-066 Cierre sin diferencia', cierre['diferencia'], 0)

    # 10. Tablero del administrador
    periodo = f'?desde={hoy}&hasta={hoy}'
    ver('HU-069 Ingresos', llamar('GET', FAC + '/api/reportes/ingresos' + periodo, token=admin)[1]['total'] >= total, True)
    ver('HU-072 Citas atendidas', llamar('GET', AGE + '/api/reportes/citas' + periodo, token=admin)[1]['atendidas'] >= 1, True)
    ver('HU-072 Ventas de farmacia', llamar('GET', FAR + '/api/reportes/ventas-farmacia' + periodo, token=admin)[1]['valor'] >= 21000, True)
    ver('HU-073 Ocupación', llamar('GET', f'{AGE}/api/reportes/ocupacion{periodo}&veterinarioId={vet_id}', token=admin)[1][0]['porcentaje'], 100)
    ver('HU-009 Auditoría de facturación', llamar('GET', FAC + '/api/auditoria?accion=GENERAR_FACTURA', token=admin)[1]['totalElementos'] >= 1, True)

    print('RESULTADO:', 'TODO OK' if not fallos else f'{len(fallos)} FALLOS: {fallos}')
    sys.exit(1 if fallos else 0)


if __name__ == '__main__':
    principal()
