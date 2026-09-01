package cl.duoc.bancoxyz.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estado_cuenta_resumen")
public class EstadoCuentaResumen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long cuentaId;
    private Integer anio;
    private Double totalIngresos;
    private Double totalEgresos;
    private Double saldoAnual;
    private Long cantidadMovimientos;

    public EstadoCuentaResumen() {
    }

    public EstadoCuentaResumen(Long cuentaId, Integer anio,
                               Double totalIngresos, Double totalEgresos,
                               Double saldoAnual, Long cantidadMovimientos) {
        this.cuentaId = cuentaId;
        this.anio = anio;
        this.totalIngresos = totalIngresos;
        this.totalEgresos = totalEgresos;
        this.saldoAnual = saldoAnual;
        this.cantidadMovimientos = cantidadMovimientos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public void setCuentaId(Long cuentaId) {
        this.cuentaId = cuentaId;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public Double getTotalIngresos() {
        return totalIngresos;
    }

    public void setTotalIngresos(Double totalIngresos) {
        this.totalIngresos = totalIngresos;
    }

    public Double getTotalEgresos() {
        return totalEgresos;
    }

    public void setTotalEgresos(Double totalEgresos) {
        this.totalEgresos = totalEgresos;
    }

    public Double getSaldoAnual() {
        return saldoAnual;
    }

    public void setSaldoAnual(Double saldoAnual) {
        this.saldoAnual = saldoAnual;
    }

    public Long getCantidadMovimientos() {
        return cantidadMovimientos;
    }

    public void setCantidadMovimientos(Long cantidadMovimientos) {
        this.cantidadMovimientos = cantidadMovimientos;
    }
}