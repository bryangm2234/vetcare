import { api } from '../api/cliente';
import { useUsuario } from '../auth/sesion';
import { NOMBRE_ROL } from '../auth/roles';
import { useDialogo } from '../componentes/Formulario';
import { Boton, Encabezado } from '../componentes/ui';

export function Cuenta() {
  const usuario = useUsuario();
  const { abrir, elemento } = useDialogo();
  return (
    <>
      <Encabezado titulo="Mi cuenta" />
      <div className="tarjeta tarjeta-contenido">
        <dl className="datos">
          <div><dt>Correo</dt><dd>{usuario.correo}</dd></div>
          <div><dt>Rol</dt><dd>{NOMBRE_ROL[usuario.rol]}</dd></div>
          <div><dt>La sesión expira</dt><dd>{new Date(usuario.expira).toLocaleTimeString('es-CO', { timeStyle: 'short' })}</dd></div>
        </dl>
        <p style={{ marginTop: 16 }}>
          <Boton tono="principal" onClick={() => abrir({
            titulo: 'Cambiar contraseña',
            exito: 'Contraseña actualizada',
            campos: [
              { nombre: 'contrasenaActual', etiqueta: 'Contraseña actual', tipo: 'password', requerido: true },
              { nombre: 'contrasenaNueva', etiqueta: 'Contraseña nueva', tipo: 'password', requerido: true, ayuda: 'Mínimo 8 caracteres' },
            ],
            enviar: (v) => api('pacientes', '/api/usuarios/me/contrasena', 'PUT', v),
          })}>Cambiar contraseña</Boton>
        </p>
      </div>
      {elemento}
    </>
  );
}
