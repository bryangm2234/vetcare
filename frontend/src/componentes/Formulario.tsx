import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { ApiError } from '../api/cliente';
import { useAvisos } from './avisos';

export interface Opcion {
  valor: string;
  texto: string;
}

export interface Campo {
  nombre: string;
  etiqueta: string;
  tipo?: 'text' | 'email' | 'password' | 'number' | 'date' | 'time' | 'textarea' | 'select' | 'checkbox' | 'filas';
  requerido?: boolean;
  opciones?: Opcion[];
  /** Opciones que dependen de otros campos, como las franjas libres de una fecha. */
  opcionesDe?: (valores: Valores) => Promise<Opcion[]>;
  dependeDe?: string[];
  ayuda?: string;
  minimo?: number;
  visible?: (valores: Valores) => boolean;
  subcampos?: Campo[];
}

export type Valores = Record<string, any>;

export interface PropsDialogo {
  titulo: string;
  descripcion?: ReactNode;
  campos?: Campo[];
  inicial?: Valores;
  textoEnviar?: string;
  peligro?: boolean;
  exito?: string;
  enviar: (valores: Valores) => Promise<unknown>;
  despues?: (resultado: any) => void;
}

function valorInicial(campos: Campo[], inicial: Valores): Valores {
  const valores: Valores = {};
  campos.forEach((c) => {
    valores[c.nombre] = inicial[c.nombre] ?? (c.tipo === 'filas' ? [vacia(c)] : c.tipo === 'checkbox' ? false : '');
  });
  return valores;
}

function vacia(campo: Campo): Valores {
  return valorInicial(campo.subcampos ?? [], {});
}

function Control({ campo, id, valor, valores, error, cambiar }: { campo: Campo; id: string; valor: any; valores: Valores; error?: string; cambiar: (v: any) => void }) {
  const [dinamicas, setDinamicas] = useState<Opcion[] | null>(null);
  const clave = (campo.dependeDe ?? []).map((d) => valores[d]).join('|');
  const obtener = useRef(campo.opcionesDe);
  obtener.current = campo.opcionesDe;

  useEffect(() => {
    if (!obtener.current) {
      return;
    }
    let vigente = true;
    setDinamicas(null);
    obtener.current(valores)
      .then((o) => { if (vigente) { setDinamicas(o); } })
      .catch(() => { if (vigente) { setDinamicas([]); } });
    return () => { vigente = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave]);

  const comunes = { id, required: campo.requerido, 'aria-invalid': Boolean(error), 'aria-describedby': error ? `${id}-error` : undefined };
  if (campo.tipo === 'textarea') {
    return <textarea {...comunes} rows={3} value={valor} onChange={(e) => cambiar(e.target.value)} />;
  }
  if (campo.tipo === 'select') {
    const opciones = campo.opcionesDe ? dinamicas ?? [] : campo.opciones ?? [];
    const sinOpciones = Boolean(campo.opcionesDe) && dinamicas !== null && opciones.length === 0;
    return (
      <select {...comunes} value={valor} onChange={(e) => cambiar(e.target.value)}>
        <option value="">{campo.opcionesDe && dinamicas === null ? 'Cargando…' : sinOpciones ? 'Sin opciones disponibles' : 'Selecciona una opción'}</option>
        {opciones.map((o) => <option key={o.valor} value={o.valor}>{o.texto}</option>)}
      </select>
    );
  }
  if (campo.tipo === 'checkbox') {
    return <input id={id} type="checkbox" checked={Boolean(valor)} onChange={(e) => cambiar(e.target.checked)} />;
  }
  return <input {...comunes} type={campo.tipo ?? 'text'} min={campo.minimo} step={campo.tipo === 'number' ? 'any' : undefined} value={valor} onChange={(e) => cambiar(e.target.value)} />;
}

export function Dialogo({ titulo, descripcion, campos = [], inicial = {}, textoEnviar = 'Guardar', peligro, exito, enviar, despues, cerrar }: PropsDialogo & { cerrar: () => void }) {
  const [valores, setValores] = useState<Valores>(() => valorInicial(campos, inicial));
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [detalles, setDetalles] = useState<Record<string, string>>({});
  const avisos = useAvisos();
  const referencia = useRef<HTMLFormElement>(null);

  useEffect(() => {
    referencia.current?.querySelector<HTMLElement>('input, select, textarea, button.boton-principal')?.focus();
    const alPulsar = (e: KeyboardEvent) => { if (e.key === 'Escape') { cerrar(); } };
    document.addEventListener('keydown', alPulsar);
    return () => document.removeEventListener('keydown', alPulsar);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const asignar = (nombre: string, valor: any) => setValores((v) => ({ ...v, [nombre]: valor }));

  const alEnviar = async (e: FormEvent) => {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    setDetalles({});
    try {
      const resultado = await enviar(valores);
      if (exito) {
        avisos.exito(exito);
      }
      cerrar();
      despues?.(resultado);
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'Ocurrió un error inesperado');
      if (causa instanceof ApiError) {
        setDetalles(causa.detalles);
      }
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="dialogo-fondo" onMouseDown={(e) => { if (e.target === e.currentTarget) { cerrar(); } }}>
      <form ref={referencia} className="dialogo" role="dialog" aria-modal="true" aria-label={titulo} onSubmit={alEnviar}>
        <h2>{titulo}</h2>
        {descripcion && <div className="dialogo-descripcion">{descripcion}</div>}
        {error && <p className="mensaje-error" role="alert">{error}</p>}
        <div className="campos">
          {campos.filter((c) => !c.visible || c.visible(valores)).map((campo) => {
            const id = `campo-${campo.nombre}`;
            if (campo.tipo === 'filas') {
              const filas: Valores[] = valores[campo.nombre];
              return (
                <fieldset key={campo.nombre} className="campo-filas">
                  <legend>{campo.etiqueta}</legend>
                  {filas.map((fila, indice) => (
                    <div key={indice} className="fila-campos">
                      {campo.subcampos!.map((sub) => (
                        <div key={sub.nombre} className="campo">
                          <label htmlFor={`${id}-${indice}-${sub.nombre}`}>{sub.etiqueta}</label>
                          <Control campo={sub} id={`${id}-${indice}-${sub.nombre}`} valor={fila[sub.nombre]} valores={fila}
                            cambiar={(v) => asignar(campo.nombre, filas.map((f, i) => (i === indice ? { ...f, [sub.nombre]: v } : f)))} />
                        </div>
                      ))}
                      {filas.length > 1 && (
                        <button type="button" className="boton boton-suave" aria-label={`Quitar fila ${indice + 1}`} onClick={() => asignar(campo.nombre, filas.filter((_, i) => i !== indice))}>Quitar</button>
                      )}
                    </div>
                  ))}
                  <button type="button" className="boton boton-suave" onClick={() => asignar(campo.nombre, [...filas, vacia(campo)])}>Agregar otro</button>
                </fieldset>
              );
            }
            return (
              <div key={campo.nombre} className={campo.tipo === 'checkbox' ? 'campo campo-check' : 'campo'}>
                <label htmlFor={id}>{campo.etiqueta}{campo.requerido && <span aria-hidden="true"> *</span>}</label>
                <Control campo={campo} id={id} valor={valores[campo.nombre]} valores={valores} error={detalles[campo.nombre]} cambiar={(v) => asignar(campo.nombre, v)} />
                {campo.ayuda && <small>{campo.ayuda}</small>}
                {detalles[campo.nombre] && <small id={`${id}-error`} className="campo-error">{detalles[campo.nombre]}</small>}
              </div>
            );
          })}
        </div>
        <div className="dialogo-acciones">
          <button type="button" className="boton boton-suave" onClick={cerrar}>Cancelar</button>
          <button type="submit" className={peligro ? 'boton boton-peligro' : 'boton boton-principal'} disabled={enviando}>{enviando ? 'Procesando…' : textoEnviar}</button>
        </div>
      </form>
    </div>
  );
}

/** Abre formularios y confirmaciones desde cualquier página. */
export function useDialogo() {
  const [props, setProps] = useState<PropsDialogo | null>(null);
  return {
    abrir: setProps as (p: PropsDialogo) => void,
    elemento: props ? <Dialogo key={props.titulo} {...props} cerrar={() => setProps(null)} /> : null,
  };
}
