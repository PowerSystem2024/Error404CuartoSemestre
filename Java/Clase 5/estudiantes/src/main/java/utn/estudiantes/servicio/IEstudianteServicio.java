package utn.estudiantes.servicio;

import utn.estudiantes.modelo.Estudiantes2025;
import java.util.List;

public interface IEstudianteServicio {
    public List<Estudiantes2025> listarEstudiante();
    public Estudiantes2025 buscarEstudiantePorId(Integer idEstudiante);
    public void guardarEstudiante(Estudiantes2025 estudiante);
    public void eliminarEstudiante(Estudiantes2025 estudiante);
}
