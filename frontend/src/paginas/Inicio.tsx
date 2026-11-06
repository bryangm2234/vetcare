import { Link } from 'react-router-dom';
import { Icono, Marca } from '../componentes/Icono';

const RECORRIDO = [
  { paso: 'Cita', detalle: 'Se agenda según los cupos del veterinario.' },
  { paso: 'Consulta', detalle: 'Signos vitales, examen, diagnóstico y plan.' },
  { paso: 'Laboratorio', detalle: 'Las órdenes y sus resultados vuelven a la historia.' },
  { paso: 'Fórmula', detalle: 'Farmacia despacha solo si está vigente.' },
  { paso: 'Hospitalización', detalle: 'Evolución del paciente hasta el alta, si hace falta.' },
  { paso: 'Facturación', detalle: 'Todos los servicios en una sola factura.' },
];

const MODULOS = [
  { icono: 'calendario', titulo: 'Recepción y agenda', texto: 'Registra propietarios y mascotas, consulta la disponibilidad, agenda citas y marca la llegada de cada paciente.' },
  { icono: 'estetoscopio', titulo: 'Historia clínica', texto: 'Una historia única por mascota, con consultas, alergias, vacunas, exámenes, fórmulas y archivos adjuntos.' },
  { icono: 'tubo', titulo: 'Laboratorio y hospitalización', texto: 'Ordena exámenes, registra resultados y da seguimiento a los pacientes internados hasta el alta.' },
  { icono: 'capsula', titulo: 'Farmacia e inventario', texto: 'Controla lotes y vencimientos, despacha fórmulas y consulta el kardex de cada medicamento.' },
  { icono: 'factura', titulo: 'Facturación y caja', texto: 'Consolida los servicios, aplica descuentos e impuestos, recibe pagos combinados y cuadra la caja del turno.' },
  { icono: 'patita', titulo: 'Portal del propietario', texto: 'Cada propietario solicita citas y consulta el carné, los resultados, las fórmulas y las facturas de sus mascotas.' },
];

const REGLAS = [
  { icono: 'reloj', titulo: 'Cupos por veterinario', texto: 'Cada veterinario tiene su horario y sus cupos diarios. No se agenda sin cupo ni en una franja ocupada.' },
  { icono: 'jeringa', titulo: 'Vacunas con fecha', texto: 'Cada especie tiene su esquema. Al aplicar una dosis se calcula la siguiente y se alertan las próximas o vencidas.' },
  { icono: 'escudo', titulo: 'Solo con fórmula vigente', texto: 'Farmacia no despacha fórmulas vencidas o anuladas, ni más de lo formulado.' },
  { icono: 'caja', titulo: 'Inventario confiable', texto: 'Sale primero el lote que vence antes, el stock nunca queda negativo y se alerta al llegar al mínimo.' },
];

const ROLES = ['Recepción', 'Veterinario', 'Auxiliar veterinario', 'Laboratorio', 'Farmacia', 'Peluquería y estética', 'Caja', 'Propietario', 'Proveedor', 'Administrador'];

export function Inicio() {
  return (
    <div className="inicio">
      <header className="inicio-cabecera">
        <Link to="/" className="inicio-logo" aria-label="VetCare, página de inicio"><Marca /> VetCare</Link>
        <nav aria-label="Secciones de la página">
          <a href="#recorrido">Recorrido</a>
          <a href="#modulos">Módulos</a>
          <a href="#reglas">Seguridad clínica</a>
          <a href="#roles">Roles</a>
        </nav>
        <Link to="/login" className="boton boton-principal">Iniciar sesión</Link>
      </header>

      <main>
        <section className="inicio-portada">
          <div className="inicio-portada-interior">
            <div>
              <p className="inicio-antetitulo">Sistema de gestión para clínicas veterinarias</p>
              <h1>La atención de cada mascota, conectada de la cita al pago.</h1>
              <p className="inicio-entrada">VetCare reúne en un solo lugar la agenda, la historia clínica, el laboratorio, la farmacia y la facturación, para que cada área trabaje con la misma información.</p>
              <div className="inicio-botones">
                <Link to="/login" className="boton boton-acento boton-grande">Iniciar sesión</Link>
                <a href="#modulos" className="boton boton-claro boton-grande">Conocer los módulos</a>
              </div>
            </div>
            <ol className="inicio-recorrido" id="recorrido" aria-label="Recorrido de atención">
              {RECORRIDO.map((r, i) => (
                <li key={r.paso}>
                  <span className="inicio-numero" aria-hidden="true">{i + 1}</span>
                  <div><strong>{r.paso}</strong><span>{r.detalle}</span></div>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section className="inicio-seccion" id="modulos">
          <div className="inicio-titulo-seccion">
            <h2>Un módulo para cada área de la clínica</h2>
            <p>Desde que el propietario pide la cita hasta que paga la factura, cada paso deja su registro y queda listo para el siguiente.</p>
          </div>
          <div className="inicio-rejilla">
            {MODULOS.map((m) => (
              <article key={m.titulo} className="inicio-tarjeta">
                <span className="inicio-icono"><Icono nombre={m.icono} tamano={22} /></span>
                <h3>{m.titulo}</h3>
                <p>{m.texto}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="inicio-seccion inicio-seccion-oscura" id="reglas">
          <div className="inicio-seccion-interior">
            <div className="inicio-titulo-seccion">
              <h2>Reglas que protegen a las mascotas y a la clínica</h2>
              <p>El sistema las aplica siempre, sin importar quién lo use.</p>
            </div>
            <div className="inicio-reglas">
              {REGLAS.map((r) => (
                <article key={r.titulo}>
                  <Icono nombre={r.icono} tamano={24} />
                  <div>
                    <h3>{r.titulo}</h3>
                    <p>{r.texto}</p>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="inicio-seccion" id="roles">
          <div className="inicio-titulo-seccion">
            <h2>Cada persona ve solo lo que le corresponde</h2>
            <p>VetCare maneja 10 roles para proteger la información clínica y financiera.</p>
          </div>
          <ul className="inicio-roles">
            {ROLES.map((rol) => <li key={rol}>{rol}</li>)}
          </ul>
        </section>

        <section className="inicio-llamado">
          <h2>¿Ya tienes una cuenta en VetCare?</h2>
          <p>Ingresa con el correo y la contraseña que te asignó la clínica.</p>
          <Link to="/login" className="boton boton-acento boton-grande">Iniciar sesión</Link>
        </section>
      </main>

      <footer className="inicio-pie">
        <span>VetCare</span>
        <span>Smart Technology – Empresa SAS · Bucaramanga, Colombia</span>
      </footer>
    </div>
  );
}
