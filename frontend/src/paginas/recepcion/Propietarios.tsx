import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api/cliente';
import { useUsuario } from '../../auth/sesion';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Tabla } from '../../componentes/Tabla';
import { Boton, CampoFiltro, Encabezado, Estado, Seccion } from '../../componentes/ui';
import { consulta, fecha, hoy } from '../../util';

function Mascotas({ propietario }: { propietario: any }) {
  const { rol } = useUsuario();
  const navegar = useNavigate();
  const mascotas = useDatos(() => api<any[]>('pacientes', `/api/propietarios/${propietario.id}/mascotas`), [propietario.id]);
  const { abrir, elemento } = useDialogo();
  const recepcion = rol === 'RECEPCIONISTA';

  const nueva = () => abrir({
    titulo: `Registrar mascota de ${propietario.nombre}`, exito: 'Mascota registrada', despues: mascotas.recargar,
    campos: [
      { nombre: 'nombre', etiqueta: 'Nombre', requerido: true },
      { nombre: 'especie', etiqueta: 'Especie', requerido: true, ayuda: 'Debe coincidir con los esquemas de vacunación, por ejemplo Canino.' },
      { nombre: 'raza', etiqueta: 'Raza', requerido: true },
      { nombre: 'sexo', etiqueta: 'Sexo', tipo: 'select', requerido: true, opciones: [{ valor: 'MACHO', texto: 'Macho' }, { valor: 'HEMBRA', texto: 'Hembra' }] },
      { nombre: 'fechaNacimiento', etiqueta: 'Fecha de nacimiento', tipo: 'date', requerido: true },
      { nombre: 'pesoKg', etiqueta: 'Peso en kg', tipo: 'number', requerido: true },
    ],
    enviar: (v) => api('pacientes', '/api/mascotas', 'POST', { ...v, pesoKg: Number(v.pesoKg), propietarioId: propietario.id }),
  });

  const cambiarEstado = (m: any) => abrir({
    titulo: `Estado de ${m.nombre}`,
    descripcion: 'Una mascota inactiva o fallecida no admite citas. El fallecimiento no se puede revertir.',
    exito: 'Estado actualizado', despues: mascotas.recargar, peligro: true, textoEnviar: 'Cambiar estado',
    inicial: { estado: m.estado, fechaFallecimiento: hoy() },
    campos: [
      { nombre: 'estado', etiqueta: 'Estado', tipo: 'select', requerido: true, opciones: [{ valor: 'ACTIVA', texto: 'Activa' }, { valor: 'INACTIVA', texto: 'Inactiva' }, { valor: 'FALLECIDA', texto: 'Fallecida' }] },
      { nombre: 'fechaFallecimiento', etiqueta: 'Fecha de fallecimiento', tipo: 'date', requerido: true, visible: (v) => v.estado === 'FALLECIDA' },
    ],
    enviar: (v) => api('pacientes', `/api/mascotas/${m.id}/estado`, 'PATCH', { estado: v.estado, fechaFallecimiento: v.estado === 'FALLECIDA' ? v.fechaFallecimiento : null }),
  });

  const transferir = (m: any) => abrir({
    titulo: `Transferir a ${m.nombre}`, exito: 'Mascota transferida', despues: mascotas.recargar, textoEnviar: 'Transferir',
    campos: [{ nombre: 'documento', etiqueta: 'Documento del nuevo propietario', requerido: true }],
    enviar: async (v) => {
      const resultado = await api('pacientes', '/api/propietarios' + consulta({ documento: v.documento.trim() }));
      if (resultado.contenido.length === 0) {
        throw new Error('No existe un propietario con ese documento');
      }
      return api('pacientes', `/api/mascotas/${m.id}/transferencia`, 'POST', { nuevoPropietarioId: resultado.contenido[0].id });
    },
  });

  const antecedentes = (m: any) => abrir({
    titulo: `Antecedentes de ${m.nombre}`, exito: 'Antecedentes guardados', despues: mascotas.recargar,
    inicial: { alergias: m.alergias.join(', '), condicionesCronicas: m.condicionesCronicas.join(', ') },
    campos: [
      { nombre: 'alergias', etiqueta: 'Alergias', ayuda: 'Separa cada una con coma' },
      { nombre: 'condicionesCronicas', etiqueta: 'Condiciones crónicas', ayuda: 'Separa cada una con coma' },
    ],
    enviar: (v) => {
      const lista = (t: string) => t.split(',').map((x) => x.trim()).filter(Boolean);
      return api('pacientes', `/api/mascotas/${m.id}/antecedentes`, 'PUT', { alergias: lista(v.alergias), condicionesCronicas: lista(v.condicionesCronicas) });
    },
  });

  return (
    <Seccion titulo={`Mascotas de ${propietario.nombre}`} acciones={recepcion && <Boton tono="principal" icono="mas" onClick={nueva}>Registrar mascota</Boton>}>
      <Tabla filas={mascotas.datos} cargando={mascotas.cargando} error={mascotas.error} clave={(m) => m.id} vacio="Este propietario aún no tiene mascotas."
        columnas={[
          { titulo: 'Nombre', valor: (m) => m.nombre },
          { titulo: 'Especie y raza', valor: (m) => `${m.especie}, ${m.raza}` },
          { titulo: 'Nacimiento', valor: (m) => fecha(m.fechaNacimiento) },
          { titulo: 'Alergias', valor: (m) => m.alergias.join(', ') || 'Ninguna registrada' },
          { titulo: 'Estado', celda: (m) => <Estado valor={m.estado} /> },
        ]}
        acciones={(m) => <>
          {recepcion && <Boton onClick={() => cambiarEstado(m)}>Estado</Boton>}
          {recepcion && <Boton onClick={() => transferir(m)}>Transferir</Boton>}
          {rol === 'VETERINARIO' && <Boton onClick={() => antecedentes(m)}>Antecedentes</Boton>}
          {rol === 'VETERINARIO' && <Boton onClick={() => navegar(`/historia/${m.id}`)}>Historia</Boton>}
        </>} />
      {elemento}
    </Seccion>
  );
}

export function Propietarios() {
  const { rol } = useUsuario();
  const [documento, setDocumento] = useState('');
  const [nombre, setNombre] = useState('');
  const [pagina, setPagina] = useState(0);
  const [elegido, setElegido] = useState<any>(null);
  const { datos, cargando, error, recargar } = useDatos(() => api('pacientes', '/api/propietarios' + consulta({ documento: documento.trim(), nombre: nombre.trim(), page: pagina, size: 8 })), [documento, nombre, pagina]);
  const { abrir, elemento } = useDialogo();
  const recepcion = rol === 'RECEPCIONISTA';

  const campos = [
    { nombre: 'telefono', etiqueta: 'Teléfono', requerido: true },
    { nombre: 'correo', etiqueta: 'Correo', tipo: 'email' as const, requerido: true },
    { nombre: 'direccion', etiqueta: 'Dirección', requerido: true },
  ];

  return (
    <>
      <Encabezado titulo="Propietarios y mascotas" descripcion="Busca por documento exacto o por parte del nombre, sin importar tildes.">
        {recepcion && <Boton tono="principal" icono="mas" onClick={() => abrir({
          titulo: 'Registrar propietario', exito: 'Propietario registrado', despues: (p) => { recargar(); setElegido(p); },
          campos: [{ nombre: 'documento', etiqueta: 'Documento', requerido: true }, { nombre: 'nombre', etiqueta: 'Nombre completo', requerido: true }, ...campos],
          enviar: (v) => api('pacientes', '/api/propietarios', 'POST', v),
        })}>Registrar propietario</Boton>}
      </Encabezado>
      <Seccion titulo="Propietarios">
        <Tabla filas={datos?.contenido} cargando={cargando} error={error} clave={(p: any) => p.id} vacio="No se encontraron propietarios."
          servidor={datos ? { pagina, totalPaginas: Math.max(datos.totalPaginas, 1), total: datos.totalElementos, cambiar: setPagina } : undefined}
          filtros={<>
            <CampoFiltro etiqueta="Documento"><input type="search" value={documento} onChange={(e) => { setDocumento(e.target.value); setPagina(0); }} /></CampoFiltro>
            <CampoFiltro etiqueta="Nombre"><input type="search" value={nombre} onChange={(e) => { setNombre(e.target.value); setPagina(0); }} /></CampoFiltro>
          </>}
          columnas={[
            { titulo: 'Documento', valor: (p: any) => p.documento },
            { titulo: 'Nombre', valor: (p: any) => p.nombre },
            { titulo: 'Teléfono', valor: (p: any) => p.telefono },
            { titulo: 'Correo', valor: (p: any) => p.correo },
          ]}
          acciones={(p: any) => <>
            <Boton onClick={() => setElegido(p)}>Mascotas</Boton>
            {recepcion && <Boton onClick={() => abrir({
              titulo: `Contacto de ${p.nombre}`, exito: 'Datos actualizados', despues: recargar,
              inicial: { telefono: p.telefono, correo: p.correo, direccion: p.direccion }, campos,
              enviar: (v) => api('pacientes', `/api/propietarios/${p.id}/contacto`, 'PATCH', v),
            })}>Editar</Boton>}
          </>} />
      </Seccion>
      {elegido && <Mascotas propietario={elegido} />}
      {elemento}
    </>
  );
}
