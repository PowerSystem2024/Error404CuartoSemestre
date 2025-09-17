package utn.estudiantes.modelo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
//boilerplate - codigo Repetitivo
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class Estudiantes2025 {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idestudiantes2025;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
}

