package com.andinaseguros.interfaceadapters.out.persistence.mongodb.document;

import java.math.BigDecimal;

public class FactorRiesgoDocument {
    public String id;
    public String codigo;
    public String nombre;
    public String tipoVariable;
    public BigDecimal valorMinimo;
    public BigDecimal valorMaximo;
    public BigDecimal multiplicador;
    public int orden;
}
