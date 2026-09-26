export type Role='ADMIN'|'ACTUARIO'|'AGENTE'|'CLIENTE';
export interface GoogleCredentialResponse{credential:string}
export interface TokenResponse{token:string;tipo:string;expiraEnSegundos:number}
export interface ResultadoLogin{requiresMfa:boolean;token:TokenResponse|null;challengeToken:string|null;challengeExpiresIn:number}
export interface MfaSetup{secret:string;otpauthUri:string;qrCodeDataUri:string}
export type TipoVehiculo='AUTO'|'CAMIONETA'|'MOTO'; export type TipoUso='PARTICULAR'|'TAXI'|'CARGA';
export interface Cliente {id:string;tipoDocumento:string;numeroDocumento:string;nombres:string;apellidos:string;fechaNacimiento:string;correo:string;telefono:string;activo:boolean}
export interface Vehiculo{id:string;clienteId:string;placa:string;marca:string;modelo:string;anioFabricacion:number;tipo:TipoVehiculo;uso:TipoUso;zonaCirculacion:string}
export interface Factor{codigo:string;nombre:string;tipoVariable:string;valorMinimo:number;valorMaximo:number;multiplicador:number;orden:number}
export interface Tabla{id:string;codigo:string;version:number;tipoVehiculo:TipoVehiculo;tipoUso:TipoUso;primaBase:number;primaMinima:number;inicio:string;fin:string;notaTecnica:string;estado:string}
export interface TablaDetalle extends Tabla{factores:Factor[]}
export type EstadoCotizacion='VIGENTE'|'ACEPTADA'|'EMITIDA'|'VENCIDA';
export interface Cotizacion{id:string;numero:string;clienteId:string;vehiculoId:string;prima:number;moneda:string;creada:string;expira:string;estado:EstadoCotizacion;desglose?:unknown}
export interface Poliza{id:string;numero:string;cotizacionId?:string;clienteId:string;vehiculoId:string;prima:number;moneda:string;inicio:string;fin:string;estado:string;renovacionOrigenId?:string}
export interface Siniestro{id:string;polizaId:string;fecha:string;tipo:string;montoEstimado:number;responsabilidadAsegurado:boolean;gravedad:string;estado:string}
export interface Renovacion{id:string;polizaOrigenId:string;primaAnterior:number;nuevaPrima:number;porcentajeVariacion:number;siniestrosConsiderados:number;estado:string;motivo:string;creadaEn:string;venceEn:string;decididaEn?:string;polizaRenovadaId?:string}
export interface MiCuentaPoliza{poliza:Poliza;renovaciones:Renovacion[]}
export interface MiCuenta{cliente:Cliente|null;polizas:MiCuentaPoliza[]}
