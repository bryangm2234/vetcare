import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSesion } from '../auth/sesion';
import { Icono, Marca } from '../componentes/Icono';

export function Login() {
  const { iniciar, aviso, cerrar } = useSesion();
  const navegar = useNavigate();
  const [correo, setCorreo] = useState('');
  const [contrasena, setContrasena] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  const enviar = async (e: FormEvent) => {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    try {
      await iniciar(correo.trim(), contrasena);
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo iniciar sesión');
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="acceso">
      <section className="acceso-marca">
        <div className="acceso-logo"><Marca tamano={40} /> VetCare</div>
        <h1>Toda la atención de tu clínica, de la cita al pago, en un solo lugar.</h1>
        <ol>
          {['Cita', 'Consulta', 'Laboratorio', 'Fórmula', 'Hospitalización', 'Facturación'].map((paso) => <li key={paso}>{paso}</li>)}
        </ol>
      </section>
      <main className="acceso-formulario">
        <form onSubmit={enviar}>
          {/* cerrar() limpia el aviso de sesión expirada, que de otro modo devolvería al acceso. */}
          <button type="button" className="boton boton-suave acceso-regresar" onClick={() => { cerrar(); navegar('/'); }}>
            <Icono nombre="regresar" />
            Regresar
          </button>
          <div>
            <h2>Iniciar sesión</h2>
            <p>Ingresa con tu correo y contraseña.</p>
          </div>
          {aviso && <p className="mensaje-info" role="status">{aviso}</p>}
          {error && <p className="mensaje-error" role="alert">{error}</p>}
          <div className="campo">
            <label htmlFor="correo">Correo</label>
            <input id="correo" type="email" autoComplete="username" required autoFocus value={correo} onChange={(e) => setCorreo(e.target.value)} />
          </div>
          <div className="campo">
            <label htmlFor="contrasena">Contraseña</label>
            <input id="contrasena" type="password" autoComplete="current-password" required value={contrasena} onChange={(e) => setContrasena(e.target.value)} />
          </div>
          <button type="submit" className="boton boton-principal" disabled={enviando}>{enviando ? 'Ingresando…' : 'Ingresar'}</button>
        </form>
      </main>
    </div>
  );
}
