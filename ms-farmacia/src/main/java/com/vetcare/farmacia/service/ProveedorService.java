package com.vetcare.farmacia.service;

import com.vetcare.farmacia.dto.ItemOrdenRequest;
import com.vetcare.farmacia.dto.ItemRecepcionRequest;
import com.vetcare.farmacia.dto.OrdenCompraRequest;
import com.vetcare.farmacia.dto.ProveedorRequest;
import com.vetcare.farmacia.dto.RecepcionRequest;
import com.vetcare.farmacia.exception.ConflictoException;
import com.vetcare.farmacia.exception.RecursoNoEncontradoException;
import com.vetcare.farmacia.exception.ReglaNegocioException;
import com.vetcare.farmacia.model.EstadoOrdenCompra;
import com.vetcare.farmacia.model.ItemOrdenCompra;
import com.vetcare.farmacia.model.OrdenCompra;
import com.vetcare.farmacia.model.Proveedor;
import com.vetcare.farmacia.repository.LoteRepository;
import com.vetcare.farmacia.repository.OrdenCompraRepository;
import com.vetcare.farmacia.repository.ProveedorRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final OrdenCompraRepository ordenRepository;
    private final LoteRepository loteRepository;
    private final MedicamentoService medicamentoService;
    private final InventarioService inventarioService;
    private final AuditoriaService auditoriaService;

    public ProveedorService(ProveedorRepository proveedorRepository, OrdenCompraRepository ordenRepository, LoteRepository loteRepository,
                            MedicamentoService medicamentoService, InventarioService inventarioService, AuditoriaService auditoriaService) {
        this.proveedorRepository = proveedorRepository;
        this.ordenRepository = ordenRepository;
        this.loteRepository = loteRepository;
        this.medicamentoService = medicamentoService;
        this.inventarioService = inventarioService;
        this.auditoriaService = auditoriaService;
    }

    public Proveedor registrar(ProveedorRequest solicitud) {
        String nit = solicitud.nit().trim();
        if (proveedorRepository.existsByNit(nit)) {
            throw new ConflictoException("El NIT ya existe");
        }
        Proveedor proveedor = new Proveedor();
        proveedor.setNit(nit);
        proveedor.setNombre(solicitud.nombre().trim());
        proveedor.setTelefono(solicitud.telefono().trim());
        proveedor.setCorreo(solicitud.correo().trim());
        return proveedorRepository.save(proveedor);
    }

    public List<Proveedor> listar() {
        return proveedorRepository.findAll();
    }

    public OrdenCompra crearOrden(OrdenCompraRequest solicitud, String usuario) {
        if (!proveedorRepository.existsById(solicitud.proveedorId())) {
            throw new RecursoNoEncontradoException("El proveedor no existe");
        }
        OrdenCompra orden = new OrdenCompra();
        orden.setProveedorId(solicitud.proveedorId());
        orden.setCreadaPor(usuario);
        orden.setItems(solicitud.items().stream().map(this::item).toList());
        OrdenCompra guardada = ordenRepository.save(orden);
        auditoriaService.registrar("CREAR_ORDEN_COMPRA", "ordenes-compra/" + guardada.getId(), "EXITOSO");
        return guardada;
    }

    public List<OrdenCompra> listarOrdenes() {
        return ordenRepository.findAll();
    }

    public List<OrdenCompra> ordenesDe(String proveedorId) {
        return ordenRepository.findByProveedorIdOrderByFechaDesc(proveedorId);
    }

    public OrdenCompra confirmar(String id, String proveedorId) {
        OrdenCompra orden = buscarOrden(id);
        if (!orden.getProveedorId().equals(proveedorId)) {
            throw new AccessDeniedException("La orden pertenece a otro proveedor");
        }
        if (orden.getEstado() != EstadoOrdenCompra.ENVIADA) {
            throw new ReglaNegocioException("La orden ya fue confirmada");
        }
        orden.setEstado(EstadoOrdenCompra.CONFIRMADA);
        return ordenRepository.save(orden);
    }

    /** HU-060: valida toda la recepción antes de modificar el inventario. */
    public OrdenCompra recibir(String id, RecepcionRequest solicitud, String responsable) {
        OrdenCompra orden = buscarOrden(id);
        if (orden.getEstado() != EstadoOrdenCompra.CONFIRMADA && orden.getEstado() != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
            throw new ReglaNegocioException("La orden no está confirmada o ya fue recibida");
        }
        Map<String, Integer> enEstaRecepcion = new HashMap<>();
        for (ItemRecepcionRequest item : solicitud.items()) {
            ItemOrdenCompra pedido = orden.getItems().stream()
                    .filter(i -> i.getMedicamentoId().equals(item.medicamentoId()))
                    .findFirst()
                    .orElseThrow(() -> new ReglaNegocioException("El medicamento no está en la orden"));
            int acumulado = enEstaRecepcion.merge(item.medicamentoId(), item.cantidad(), Integer::sum);
            if (pedido.getRecibida() + acumulado > pedido.getCantidad()) {
                throw new ReglaNegocioException("La cantidad recibida supera la pendiente");
            }
            if (item.vencimiento().isBefore(LocalDate.now())) {
                throw new ReglaNegocioException("La fecha de vencimiento ya pasó");
            }
            if (loteRepository.existsByMedicamentoIdAndNumero(item.medicamentoId(), item.lote().trim())) {
                throw new ConflictoException("El lote ya existe para ese medicamento");
            }
        }
        String referencia = "Orden de compra " + id;
        for (ItemRecepcionRequest item : solicitud.items()) {
            inventarioService.registrarLote(item.medicamentoId(), item.lote(), item.vencimiento(), item.cantidad(), responsable, referencia);
        }
        orden.getItems().forEach(i -> i.setRecibida(i.getRecibida() + enEstaRecepcion.getOrDefault(i.getMedicamentoId(), 0)));
        boolean completa = orden.getItems().stream().allMatch(i -> i.getRecibida() >= i.getCantidad());
        orden.setEstado(completa ? EstadoOrdenCompra.RECIBIDA : EstadoOrdenCompra.RECIBIDA_PARCIAL);
        OrdenCompra guardada = ordenRepository.save(orden);
        auditoriaService.registrar("RECIBIR_MERCANCIA", "ordenes-compra/" + id, guardada.getEstado().name());
        return guardada;
    }

    private ItemOrdenCompra item(ItemOrdenRequest solicitud) {
        medicamentoService.buscar(solicitud.medicamentoId());
        ItemOrdenCompra item = new ItemOrdenCompra();
        item.setMedicamentoId(solicitud.medicamentoId());
        item.setCantidad(solicitud.cantidad());
        return item;
    }

    private OrdenCompra buscarOrden(String id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La orden de compra no existe"));
    }
}
