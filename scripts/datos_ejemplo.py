"""Carga usuarios y datos de ejemplo por rol en los servicios locales, para probar la interfaz.

Solo acepta servicios en localhost. Genera una contraseña aleatoria y la guarda en
usuarios-ejemplo.local.md, que Git ignora. No imprime credenciales.
Uso, con los 5 microservicios en ejecución y desde la raíz del repositorio:
    python scripts/datos_ejemplo.py
"""
import datetime as dt
import json
import os
import secrets
import sys
import time
import urllib.error
import urllib.request
from urllib.parse import urlparse

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PAC = os.environ.get('VETCARE_URL_PACIENTES', 'http://localhost:8081')
AGE = os.environ.get('VETCARE_URL_AGENDA', 'http://localhost:8082')
HIS = os.environ.get('VETCARE_URL_HISTORIA', 'http://localhost:8083')
FAR = os.environ.get('VETCARE_URL_FARMACIA', 'http://localhost:8084')
FAC = os.environ.get('VETCARE_URL_FACTURACION', 'http://localhost:8085')
ARCHIVO = os.path.join(RAIZ, 'usuarios-ejemplo.local.md')


class ErrorApi(Exception):
    def __init__(self, estado, cuerpo):
        super().__init__(f'{estado} {cuerpo}')
        self.estado = estado


def llamar(metodo, url, cuerpo=None, token=None):
    cabeceras = {'Content-Type': 'application/json'}
    if token:
        cabeceras['Authorization'] = 'Bearer ' + token
    peticion = urllib.request.Request(url, method=metodo, data=json.dumps(cuerpo).encode() if cuerpo is not None else None, headers=cabeceras)
    try:
        with urllib.request.urlopen(peticion, timeout=30) as respuesta:
            contenido = respuesta.read()
            return json.loads(contenido) if contenido else None
    except urllib.error.HTTPError as error:
        raise ErrorApi(error.code, error.read()[:300].decode('utf-8', 'replace')) from error


def esperar_servicios():
    for url in (PAC, AGE, HIS, FAR, FAC):
        if urlparse(url).hostname not in ('localhost', '127.0.0.1'):
            raise SystemExit('Este script solo se puede usar con servicios locales.')
        limite = time.time() + 120
        while True:
            try:
                if llamar('GET', url + '/actuator/health')['status'] == 'UP':
                    break
            except (ErrorApi, OSError):
                pass
            if time.time() > limite:
                raise SystemExit(f'El servicio {url} no responde. Arráncalo antes de cargar los datos.')
            time.sleep(2)


def principal():
    esperar_servicios()
    variables = dict(l.split('=', 1) for l in open(os.path.join(RAIZ, '.env'), encoding='utf-8').read().splitlines() if '=' in l and not l.startswith('#'))
    admin = llamar('POST', PAC + '/api/auth/login', {'correo': variables['VETCARE_ADMIN_CORREO'], 'contrasena': variables['VETCARE_ADMIN_CONTRASENA']})['token']
    if llamar('GET', PAC + '/api/usuarios?rol=RECEPCIONISTA&size=100', token=admin)['totalElementos'] > 0 or os.path.exists(ARCHIVO):
        raise SystemExit('Los datos de ejemplo parecen estar cargados. No se modificó nada.')

    clave = 'Demo-' + secrets.token_urlsafe(10)
    hoy = dt.date.today()
    dia = lambda n: str(hoy + dt.timedelta(days=n))
    creados = []

    def usuario(alias, nombre, rol, **extra):
        correo = f'{alias}@vetcare.test'
        creado = llamar('POST', PAC + '/api/usuarios', dict(nombre=nombre, correo=correo, contrasena=clave, rol=rol, **extra), admin)
        creados.append((rol, nombre, correo))
        return creado['id'], llamar('POST', PAC + '/api/auth/login', {'correo': correo, 'contrasena': clave})['token']

    # Personal
    usuario('admin2', 'Patricia Luna', 'ADMINISTRADOR')
    _, recepcion = usuario('recepcion1', 'Ana Torres', 'RECEPCIONISTA')
    usuario('recepcion2', 'Diego Salas', 'RECEPCIONISTA')
    vet1_id, vet1 = usuario('veterinaria1', 'Dra. Laura Pérez', 'VETERINARIO')
    vet2_id, vet2 = usuario('veterinario2', 'Dr. Andrés Rincón', 'VETERINARIO')
    vet3_id, _ = usuario('veterinaria3', 'Dra. Camila Ortiz', 'VETERINARIO')
    _, auxiliar = usuario('auxiliar1', 'Luis Rojas', 'AUXILIAR_VETERINARIO')
    usuario('auxiliar2', 'Paula Mejía', 'AUXILIAR_VETERINARIO')
    _, laboratorio = usuario('laboratorio1', 'Marta Díaz', 'LABORATORIO')
    usuario('laboratorio2', 'Felipe Castro', 'LABORATORIO')
    _, farmacia = usuario('farmacia1', 'Pedro Ruiz', 'FARMACIA')
    usuario('farmacia2', 'Natalia Vera', 'FARMACIA')
    _, peluqueria = usuario('peluqueria1', 'Sofía Mora', 'PELUQUERIA')
    usuario('peluqueria2', 'Tomás Gil', 'PELUQUERIA')
    _, caja = usuario('caja1', 'Carlos Vega', 'CAJERO')
    usuario('caja2', 'Elena Prada', 'CAJERO')

    # Propietarios y mascotas
    def propietario(documento, nombre, telefono, correo, direccion):
        return llamar('POST', PAC + '/api/propietarios', dict(documento=documento, nombre=nombre, telefono=telefono, correo=correo, direccion=direccion), recepcion)['id']

    def mascota(nombre, especie, raza, sexo, nacimiento, peso, propietario_id):
        return llamar('POST', PAC + '/api/mascotas', dict(nombre=nombre, especie=especie, raza=raza, sexo=sexo, fechaNacimiento=nacimiento, pesoKg=peso, propietarioId=propietario_id), recepcion)['id']

    maria = propietario('1098765432', 'María Gómez', '3001234567', 'maria.gomez@correo.test', 'Calle 10 # 20-30, Bucaramanga')
    julian = propietario('1012345678', 'Julián Peña', '3109876543', 'julian.pena@correo.test', 'Carrera 5 # 12-40, Bucaramanga')
    lucia = propietario('1055667788', 'Lucía Hernández', '3154443322', 'lucia.hernandez@correo.test', 'Avenida Quebradaseca # 30-15, Bucaramanga')
    jorge = propietario('91234567', 'Jorge Ramírez', '3177778899', 'jorge.ramirez@correo.test', 'Calle 45 # 27-10, Floridablanca')
    carolina = propietario('1100223344', 'Carolina Suárez', '3201112233', 'carolina.suarez@correo.test', 'Carrera 33 # 48-20, Bucaramanga')

    max_ = mascota('Max', 'Canino', 'Criollo', 'MACHO', '2022-03-15', 12.5, maria)
    mia = mascota('Mia', 'Felino', 'Persa', 'HEMBRA', '2021-07-01', 4.2, maria)
    rex = mascota('Rex', 'Canino', 'Labrador', 'MACHO', '2020-01-20', 30, julian)
    luna = mascota('Luna', 'Canino', 'Golden Retriever', 'HEMBRA', '2023-05-10', 24, lucia)
    simba = mascota('Simba', 'Felino', 'Criollo', 'MACHO', '2019-11-02', 5.1, lucia)
    toby = mascota('Toby', 'Canino', 'Beagle', 'MACHO', '2018-08-23', 13.4, jorge)
    nala = mascota('Nala', 'Felino', 'Siamés', 'HEMBRA', '2022-12-12', 3.8, jorge)
    rocky = mascota('Rocky', 'Canino', 'Bulldog', 'MACHO', '2017-02-14', 22, carolina)
    llamar('PUT', f'{PAC}/api/mascotas/{max_}/antecedentes', {'alergias': ['Penicilina'], 'condicionesCronicas': []}, vet1)
    llamar('PUT', f'{PAC}/api/mascotas/{toby}/antecedentes', {'alergias': [], 'condicionesCronicas': ['Diabetes']}, vet1)
    llamar('PATCH', f'{PAC}/api/mascotas/{rocky}/estado', {'estado': 'INACTIVA'}, recepcion)

    usuario('propietaria1', 'María Gómez', 'PROPIETARIO', propietarioId=maria)
    usuario('propietario2', 'Julián Peña', 'PROPIETARIO', propietarioId=julian)
    usuario('propietaria3', 'Lucía Hernández', 'PROPIETARIO', propietarioId=lucia)

    # Horarios y tarifas
    laborales = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
    for vid, nombre, inicio, fin, cupos in ((vet1_id, 'Dra. Laura Pérez', '08:00', '17:00', 14), (vet2_id, 'Dr. Andrés Rincón', '08:00', '12:00', 8), (vet3_id, 'Dra. Camila Ortiz', '13:00', '18:00', 10)):
        llamar('PUT', f'{AGE}/api/horarios/{vid}', dict(nombre=nombre, dias=laborales, horaInicio=inicio, horaFin=fin, duracionMinutos=30, cuposDiarios=cupos), admin)
    llamar('PUT', AGE + '/api/peluqueria/horario', dict(nombre='Peluquería', dias=laborales, horaInicio='08:00', horaFin='17:00', duracionMinutos=30, cuposDiarios=10), admin)
    existentes = {t['servicio'] for t in llamar('GET', FAC + '/api/tarifas', token=admin)}
    for servicio, nombre, precio in (('CONSULTA', 'Consulta general', 60000), ('LABORATORIO', 'Examen de laboratorio', 45000), ('HOSPITALIZACION', 'Día de hospitalización', 90000), ('PELUQUERIA', 'Baño y estética', 39000)):
        if servicio not in existentes:
            llamar('POST', FAC + '/api/tarifas', dict(servicio=servicio, nombre=nombre, precio=precio), admin)

    # Farmacia e inventario
    proveedor1 = llamar('POST', FAR + '/api/proveedores', dict(nit='900123456-7', nombre='Distribuciones Vet SAS', telefono='6071234567', correo='ventas@distribuciones.test'), admin)['id']
    proveedor2 = llamar('POST', FAR + '/api/proveedores', dict(nit='900765432-1', nombre='Insumos Animales Ltda', telefono='6077654321', correo='pedidos@insumos.test'), admin)['id']
    usuario('proveedor1', 'Distribuciones Vet SAS', 'PROVEEDOR', proveedorId=proveedor1)
    usuario('proveedor2', 'Insumos Animales Ltda', 'PROVEEDOR', proveedorId=proveedor2)

    def medicamento(codigo, nombre, tipo, presentacion, precio, minimo, lotes):
        creado = llamar('POST', FAR + '/api/medicamentos', dict(codigo=codigo, nombre=nombre, tipo=tipo, presentacion=presentacion, precio=precio, stockMinimo=minimo), farmacia)['id']
        for numero, dias, cantidad in lotes:
            llamar('POST', f'{FAR}/api/medicamentos/{creado}/lotes', dict(numero=numero, vencimiento=dia(dias), cantidad=cantidad), farmacia)
        return creado

    metronidazol = medicamento('MED-001', 'Metronidazol 250 mg', 'MEDICAMENTO', 'Tableta', 1500, 10, [('MTZ-2601', 30, 20), ('MTZ-2602', 300, 40)])
    amoxicilina = medicamento('MED-002', 'Amoxicilina 500 mg', 'MEDICAMENTO', 'Tableta', 2200, 30, [('AMX-2601', 200, 25)])
    meloxicam = medicamento('MED-003', 'Meloxicam 2 mg', 'MEDICAMENTO', 'Tableta', 3100, 15, [('MLX-2601', 45, 12)])
    medicamento('MED-004', 'Suero fisiológico 500 ml', 'INSUMO', 'Bolsa', 8500, 5, [('SUE-2601', 400, 30)])
    medicamento('INS-001', 'Jeringa de 5 ml', 'INSUMO', 'Unidad', 600, 50, [('JER-2601', 700, 200)])
    medicamento('VAC-001', 'Vacuna antirrábica', 'MEDICAMENTO', 'Dosis', 25000, 3, [('VAC-RAB-01', 180, 15)])
    medicamento('VAC-002', 'Vacuna contra moquillo', 'MEDICAMENTO', 'Dosis', 32000, 3, [('VAC-MOQ-01', 120, 10)])
    orden1 = llamar('POST', FAR + '/api/ordenes-compra', dict(proveedorId=proveedor1, items=[dict(medicamentoId=amoxicilina, cantidad=100)]), farmacia)['id']
    llamar('POST', FAR + '/api/ordenes-compra', dict(proveedorId=proveedor1, items=[dict(medicamentoId=meloxicam, cantidad=50), dict(medicamentoId=metronidazol, cantidad=60)]), farmacia)
    llamar('POST', FAR + '/api/ordenes-compra', dict(proveedorId=proveedor2, items=[dict(medicamentoId=meloxicam, cantidad=30)]), farmacia)
    token_proveedor = llamar('POST', PAC + '/api/auth/login', {'correo': 'proveedor1@vetcare.test', 'contrasena': clave})['token']
    llamar('PATCH', f'{FAR}/api/proveedor/ordenes-compra/{orden1}/confirmacion', token=token_proveedor)

    # Vacunación: incluye dosis vencidas y próximas para ver las alertas
    for especie, vacuna, dosis, intervalo in (('Canino', 'Rabia', 2, 365), ('Canino', 'Moquillo', 3, 21), ('Felino', 'Rabia', 2, 365), ('Felino', 'Triple felina', 3, 21)):
        llamar('POST', HIS + '/api/esquemas-vacunacion', dict(especie=especie, vacuna=vacuna, dosis=dosis, intervaloDias=intervalo), vet1)

    def vacunar(mascota_id, vacuna, lote, hace):
        llamar('POST', HIS + '/api/vacunas', dict(mascotaId=mascota_id, vacuna=vacuna, lote=lote, fecha=dia(-hace)), vet1)

    vacunar(max_, 'Rabia', 'VAC-RAB-01', 30)
    vacunar(rex, 'Rabia', 'VAC-RAB-01', 390)
    vacunar(luna, 'Moquillo', 'VAC-MOQ-01', 15)
    vacunar(toby, 'Moquillo', 'VAC-MOQ-01', 30)
    vacunar(nala, 'Rabia', 'VAC-RAB-01', 200)

    # Citas de hoy y próximas, en distintos estados
    def cita(mascota_id, vet, hora, motivo, dias=0):
        return llamar('POST', AGE + '/api/citas', dict(mascotaId=mascota_id, veterinarioId=vet, fecha=dia(dias), hora=hora, motivo=motivo), recepcion)['id']

    def llegada(cita_id):
        llamar('PATCH', f'{AGE}/api/citas/{cita_id}/llegada', token=recepcion)

    def signos(cita_id, peso, temperatura):
        llamar('PUT', f'{HIS}/api/citas/{cita_id}/signos-vitales', dict(pesoKg=peso, temperatura=temperatura, frecuenciaCardiaca=95, frecuenciaRespiratoria=24), auxiliar)

    def abrir(cita_id, token):
        return llamar('POST', HIS + '/api/consultas', {'citaId': cita_id}, token)['id']

    def registrar(consulta, token, diagnostico, plan):
        llamar('PUT', f'{HIS}/api/consultas/{consulta}/registro-clinico', dict(anamnesis='Motivo de consulta referido por el propietario', examenFisico='Examen físico completo', diagnostico=diagnostico, planTratamiento=plan), token)

    # Consulta cerrada de Max: laboratorio, fórmula despachada y factura pagada
    c_max = cita(max_, vet1_id, '08:00', 'Vómito desde hace dos días')
    llegada(c_max)
    signos(c_max, 12.5, 38.6)
    con_max = abrir(c_max, vet1)
    registrar(con_max, vet1, 'Gastroenteritis', 'Dieta blanda e hidratación')
    orden_max = llamar('POST', f'{HIS}/api/consultas/{con_max}/ordenes-laboratorio', {'examen': 'Hemograma'}, vet1)['id']
    llamar('PUT', f'{HIS}/api/ordenes-laboratorio/{orden_max}/resultado', {'resultado': 'Leucocitos levemente elevados'}, laboratorio)
    formula_max = llamar('POST', HIS + '/api/formulas', dict(consultaId=con_max, vigenteHasta=dia(15), items=[dict(medicamentoId=metronidazol, dosis='1 tableta', frecuencia='Cada 12 horas', duracion='7 días', cantidad=14)]), vet1)['id']
    llamar('POST', FAR + '/api/despachos', dict(formulaId=formula_max, items=[dict(medicamentoId=metronidazol, cantidad=14)]), farmacia)
    llamar('PATCH', f'{HIS}/api/consultas/{con_max}/cierre', token=vet1)

    # Consulta cerrada de Luna: fórmula parcial, pendiente de cobrar
    c_luna = cita(luna, vet2_id, '08:30', 'Otitis')
    llegada(c_luna)
    signos(c_luna, 24, 38.9)
    con_luna = abrir(c_luna, vet2)
    registrar(con_luna, vet2, 'Otitis externa', 'Limpieza y antibiótico')
    formula_luna = llamar('POST', HIS + '/api/formulas', dict(consultaId=con_luna, vigenteHasta=dia(20), items=[dict(medicamentoId=amoxicilina, dosis='1 tableta', frecuencia='Cada 8 horas', duracion='5 días', cantidad=15)]), vet2)['id']
    llamar('POST', FAR + '/api/despachos', dict(formulaId=formula_luna, items=[dict(medicamentoId=amoxicilina, cantidad=6)]), farmacia)
    llamar('PATCH', f'{HIS}/api/consultas/{con_luna}/cierre', token=vet2)

    # Consulta abierta de Simba, con órdenes pendientes y fórmula por despachar
    c_simba = cita(simba, vet1_id, '09:00', 'Control de peso')
    llegada(c_simba)
    signos(c_simba, 5.1, 38.3)
    con_simba = abrir(c_simba, vet1)
    registrar(con_simba, vet1, 'Sobrepeso leve', 'Ajustar la dieta')
    llamar('POST', f'{HIS}/api/consultas/{con_simba}/ordenes-laboratorio', {'examen': 'Perfil renal', 'indicaciones': 'En ayunas'}, vet1)
    llamar('POST', f'{HIS}/api/consultas/{con_simba}/ordenes-laboratorio', {'examen': 'Coprológico'}, vet1)
    llamar('POST', HIS + '/api/formulas', dict(consultaId=con_simba, vigenteHasta=dia(10), items=[dict(medicamentoId=meloxicam, dosis='1 tableta', frecuencia='Cada 24 horas', duracion='3 días', cantidad=3)]), vet1)

    # Pacientes en espera, programados y futuros
    llegada(cita(toby, vet1_id, '10:00', 'Control de diabetes'))
    llegada(cita(nala, vet2_id, '09:30', 'Decaimiento'))
    cita(mia, vet1_id, '11:00', 'Control general')
    cita(rex, vet2_id, '10:30', 'Cojera')
    cita(toby, vet3_id, '14:00', 'Revisión dental')
    cancelada = cita(nala, vet3_id, '15:00', 'Vacunación')
    llamar('PATCH', f'{AGE}/api/citas/{cancelada}/cancelacion', {'motivo': 'El propietario no puede asistir'}, recepcion)
    cita(max_, vet1_id, '09:00', 'Control de gastroenteritis', 3)
    cita(luna, vet2_id, '09:00', 'Control de otitis', 5)
    cita(mia, vet3_id, '13:30', 'Vacunación', 7)

    # Hospitalización
    hospital = llamar('POST', HIS + '/api/hospitalizaciones', dict(mascotaId=rex, motivo='Observación por cojera y dolor', cubiculo='C-01'), vet1)['id']
    llamar('POST', f'{HIS}/api/hospitalizaciones/{hospital}/evoluciones', dict(observaciones='Paciente estable, come bien', temperatura=38.4), auxiliar)
    llamar('POST', HIS + '/api/hospitalizaciones', dict(mascotaId=toby, motivo='Descompensación de diabetes', cubiculo='C-02'), vet2)

    # Peluquería
    def servicio_peluqueria(mascota_id, hora, nombre, dias=0):
        return llamar('POST', AGE + '/api/peluqueria/citas', dict(mascotaId=mascota_id, fecha=dia(dias), hora=hora, servicio=nombre), peluqueria)['id']

    bano_max = servicio_peluqueria(max_, '09:00', 'Baño medicado')
    llamar('PATCH', f'{AGE}/api/peluqueria/citas/{bano_max}/servicio', {'observaciones': 'Sin novedades'}, peluqueria)
    servicio_peluqueria(mia, '10:00', 'Baño y corte')
    servicio_peluqueria(luna, '11:00', 'Corte de uñas')
    servicio_peluqueria(nala, '10:00', 'Baño', 2)

    # Caja y facturas: una pagada, una a crédito con abono y una por cobrar
    caja_id = llamar('POST', FAC + '/api/cajas/apertura', {'base': 200000}, caja)['id']
    factura_max = llamar('POST', FAC + '/api/facturas', {'mascotaId': max_}, caja)
    llamar('PATCH', f"{FAC}/api/facturas/{factura_max['id']}/ajustes", {'descuentoPorcentaje': 5}, caja)
    saldo = llamar('GET', f"{FAC}/api/facturas/{factura_max['id']}", token=caja)['saldo']
    llamar('POST', f"{FAC}/api/facturas/{factura_max['id']}/pagos", {'pagos': [dict(medio='EFECTIVO', valor=saldo - 50000), dict(medio='TARJETA', valor=50000)]}, caja)
    factura_luna = llamar('POST', FAC + '/api/facturas', {'mascotaId': luna, 'credito': True}, caja)
    llamar('POST', f"{FAC}/api/facturas/{factura_luna['id']}/abonos", dict(medio='TRANSFERENCIA', valor=40000), caja)
    assert caja_id

    # Credenciales en un archivo local que Git ignora
    filas = '\n'.join(f'| {rol} | {nombre} | `{correo}` |' for rol, nombre, correo in creados)
    with open(ARCHIVO, 'w', encoding='utf-8') as archivo:
        archivo.write(f'''# Usuarios de ejemplo de VetCare

Solo para pruebas locales. Git ignora este archivo. No uses estos usuarios en la nube.

**Contraseña de todos:** `{clave}`

| Rol | Nombre | Correo |
|---|---|---|
{filas}

El administrador original, `{variables['VETCARE_ADMIN_CORREO']}`, conserva la contraseña de tu archivo `.env`.
''')
    print('USUARIOS', len(creados))
    for rol, _, correo in creados:
        print(' ', rol, correo)
    print('Credenciales guardadas en usuarios-ejemplo.local.md')


if __name__ == '__main__':
    principal()
