import { useState } from 'react';
import { api } from '../../api/cliente';
import { useDialogo } from '../../componentes/Formulario';
import { useDatos } from '../../componentes/ganchos';
import { Boton, Encabezado, Estado } from '../../componentes/ui';
import { fechaHora, pesos } from '../../util';

export function Caja() {
  const caja = useDatos(() => api('facturacion', '/api/cajas/actual'));
  const [cierre, setCierre] = useState<any>(null);
  const { abrir, elemento } = useDialogo();
  const abierta = caja.datos && !caja.error;
  return (
    <>
      <Encabezado titulo="Caja" descripcion="Cada cajero tiene una caja abierta a la vez.">
        {!abierta && !caja.cargando && <Boton tono="principal" onClick={() => abrir({
          titulo: 'Abrir caja', exito: 'Caja abierta', textoEnviar: 'Abrir', despues: () => { setCierre(null); caja.recargar(); },
          campos: [{ nombre: 'base', etiqueta: 'Base en efectivo', tipo: 'number', minimo: 0, requerido: true }],
          enviar: (v) => api('facturacion', '/api/cajas/apertura', 'POST', { base: Number(v.base) }),
        })}>Abrir caja</Boton>}
        {abierta && <Boton onClick={() => abrir({
          titulo: 'Cerrar caja', descripcion: 'El sistema compara el efectivo esperado con el que cuentes.', textoEnviar: 'Cerrar caja', peligro: true, exito: 'Caja cerrada', despues: (c) => { setCierre(c); caja.recargar(); },
          campos: [{ nombre: 'efectivoContado', etiqueta: 'Efectivo contado', tipo: 'number', minimo: 0, requerido: true }],
          enviar: (v) => api('facturacion', `/api/cajas/${caja.datos.id}/cierre`, 'PATCH', { efectivoContado: Number(v.efectivoContado) }),
        })}>Cerrar caja</Boton>}
      </Encabezado>
      {abierta && (
        <div className="tarjeta tarjeta-contenido">
          <dl className="datos">
            <div><dt>Estado</dt><dd><Estado valor="ABIERTA" /></dd></div>
            <div><dt>Apertura</dt><dd>{fechaHora(caja.datos.apertura)}</dd></div>
            <div><dt>Base</dt><dd>{pesos(caja.datos.base)}</dd></div>
          </dl>
        </div>
      )}
      {!abierta && !caja.cargando && !cierre && <p className="tarjeta tarjeta-contenido">No tienes una caja abierta.</p>}
      {cierre && (
        <div className="tarjeta tarjeta-contenido">
          <h2 style={{ marginBottom: 12 }}>Último cierre</h2>
          <dl className="datos">
            <div><dt>Efectivo esperado</dt><dd>{pesos(cierre.efectivoEsperado)}</dd></div>
            <div><dt>Efectivo contado</dt><dd>{pesos(cierre.efectivoContado)}</dd></div>
            <div><dt>Diferencia</dt><dd>{pesos(cierre.diferencia)}</dd></div>
          </dl>
        </div>
      )}
      {elemento}
    </>
  );
}
