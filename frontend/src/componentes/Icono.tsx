const TRAZOS: Record<string, string> = {
  panel: 'M4 4h7v8H4zM13 4h7v5h-7zM13 11h7v9h-7zM4 14h7v6H4z',
  usuarios: 'M16 20v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 10a4 4 0 1 0 0-8 4 4 0 0 0 0 8M22 20v-2a4 4 0 0 0-3-3.9M16 2.1a4 4 0 0 1 0 7.8',
  patita: 'M12 21c-3.5 0-6-2-6-4.5S9 11 12 11s6 3 6 5.5-2.500 4.500-6 4.500zM5.500 12a2 2.500 0 1 0 0-5 2 2.500 0 0 0 0 5zM18.500 12a2 2.500 0 1 0 0-5 2 2.500 0 0 0 0 5zM9.500 8a2 2.500 0 1 0 0-5 2 2.500 0 0 0 0 5zM14.500 8a2 2.500 0 1 0 0-5 2 2.500 0 0 0 0 5z',
  calendario: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
  estetoscopio: 'M6 3H4v6a5 5 0 0 0 10 0V3h-2M9 14v2a5 5 0 0 0 10 0v-3M19 13a2 2 0 1 0 0-4 2 2 0 0 0 0 4',
  historia: 'M6 2h9l5 5v15H6zM15 2v5h5M9 13h8M9 17h8',
  jeringa: 'M18 2l4 4M20 4l-3.500 3.500M14 5l5 5M16 7L6 17l-3 4 4-3L17 8M10 9l2 2M7 12l2 2',
  cama: 'M3 6v14M3 16h18v4M21 16v-3a3 3 0 0 0-3-3H10v6M7 13a1.500 1.500 0 1 0 0-3 1.500 1.500 0 0 0 0 3',
  tubo: 'M9 3h6M10 3v14a2 2 0 0 0 4 0V3M10 12h4',
  capsula: 'M10.500 20.500l10-10a4.950 4.950 0 0 0-7-7l-10 10a4.950 4.950 0 0 0 7 7zM8.500 8.500l7 7',
  caja: 'M21 8l-9-5-9 5v8l9 5 9-5zM3 8l9 5 9-5M12 13v8',
  camion: 'M2 5h13v11H2zM15 9h4l3 3v4h-7M6.500 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4M17.500 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4',
  factura: 'M5 2h14v20l-3-2-2 2-2-2-2 2-2-2-3 2zM9 7h6M9 11h6M9 15h4',
  'caja-registradora': 'M3 13h18v8H3zM5 13l2-6h10l2 6M9 3h6v4H9zM8 17h8',
  grafico: 'M4 20V4M4 20h16M8 16v-4M12 16V8M16 16v-6',
  escudo: 'M12 2l8 3v6c0 5-3.500 9-8 11-4.500-2-8-6-8-11V5zM9 12l2 2 4-4',
  reloj: 'M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20M12 6v6l4 2',
  tijeras: 'M6 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6M6 21a3 3 0 1 0 0-6 3 3 0 0 0 0 6M20 4L8.100 15.900M14.500 14.500L20 20M8.100 8.100L12 12',
  etiqueta: 'M12 2H3v9l10 10 9-9zM7.500 7.500h.01',
  alerta: 'M12 3l10 18H2zM12 10v5M12 18h.01',
  check: 'M4 12l5 5L20 6',
  menu: 'M3 6h18M3 12h18M3 18h18',
  salir: 'M9 21H4V3h5M16 17l5-5-5-5M21 12H9',
  mas: 'M12 5v14M5 12h14',
  buscar: 'M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16M21 21l-4.300-4.300',
  cuenta: 'M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10M4 22a8 8 0 0 1 16 0',
  descargar: 'M12 3v12M7 10l5 5 5-5M4 21h16',
};

export function Icono({ nombre, tamano = 18 }: { nombre: string; tamano?: number }) {
  return (
    <svg className="icono" width={tamano} height={tamano} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d={TRAZOS[nombre] ?? TRAZOS.panel} />
    </svg>
  );
}

export function Marca({ tamano = 34 }: { tamano?: number }) {
  return (
    <svg width={tamano} height={tamano} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="9" fill="#d9613c" />
      <g fill="#fff8f2">
        <ellipse cx="16" cy="20.500" rx="6" ry="5" />
        <circle cx="8.500" cy="14.500" r="2.400" />
        <circle cx="13" cy="10" r="2.400" />
        <circle cx="19" cy="10" r="2.400" />
        <circle cx="23.500" cy="14.500" r="2.400" />
      </g>
    </svg>
  );
}
