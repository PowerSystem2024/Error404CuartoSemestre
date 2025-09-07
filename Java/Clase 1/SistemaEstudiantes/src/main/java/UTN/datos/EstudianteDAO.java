package UTN.datos;

import UTN.dominio.Estudiante;

import static UTN.conexion.Conexion.getConexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EstudianteDAO {
    //Métod listar
    public List<Estudiante> listarEstudiantes(){
        List<Estudiante> estudiantes = new ArrayList<>();
        //Creamos algunos objetos que son necesarios para comunicarnos con la base de datos
        PreparedStatement ps; //Enviamos la sentencia a la base de datos
        ResultSet rs; //Obtenemos el resultado
        //Creamos un objeto tipo conexion
        Connection con = getConexion();
        String sql = "SELECT * FROM estudiantes2025 ORDER BY id_estudiantes2025";
        try {
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                var estudiante = new Estudiante();
                estudiante.setIdEstudiante(rs.getInt("id_estudiantes2025"));
                estudiante.setNombre(rs.getString("nombre"));
                estudiante.setApellido(rs.getString("apellido"));
                estudiante.setEmail(rs.getString("email"));
                estudiantes.add(estudiante); //Falta agregarlo a la lista
            }
        }catch (Exception e){
            System.out.println("Ocurrio un error al seleccionar datos"+e.getMessage());
        }
        finally {
            try {
                con.close();
            }catch (Exception e){
                System.out.println("Ocurrio un error al cerrar la coneccion: "+e.getMessage());
            }
        }//Fin finally
        return estudiantes;
    }//Fin metodo Listar

    //Metodo por id -> fin by id
    public boolean buscarEstudiantePorId(Estudiante estudiante){
        PreparedStatement ps;
        ResultSet rs;
        Connection con = getConexion();
        String sql = "SELECT * FROM estudiantes2025 WHERE id_estudiantes2025=?";

        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, estudiante.getIdEstudiante());
            rs = ps.executeQuery();
            if (rs.next()){
                estudiante.setNombre (rs.getString("nombre"));
                estudiante.setApellido (rs.getString("apellido"));
                estudiante.setTelefono (rs.getString("telefono"));
                estudiante.setEmail (rs.getString("email"));
                return true; //Se encontro un registro
            }//Fin if
        }catch (Exception e){
            System.out.println("Ocurrio un error al buscar estudiante: "+e.getMessage());
        }
        finally {
            try {
                con.close();
            }catch (Exception e){
                System.out.println("Ocurrio un error al cerrar la coneccion: "+e.getMessage());
            }//Fin catch
        }//Fin finally
        return false;
    }

    //Metodo agregar un nuevo estudiante
    public boolean agregarEstudiante(Estudiante estudiante){
        PreparedStatement ps;
        Connection con = getConexion();
        String sql = "INSERT INTO estudiantes2025(nombre, apellido, telefono,email) VALUES (?, ?, ?, ?)";
        try{
            ps = con.prepareStatement(sql);
            ps.setString(1, estudiante.getNombre());
            ps.setString(2, estudiante.getApellido());
            ps.setString(3, estudiante.getTelefono());
            ps.setString(4, estudiante.getEmail());
            ps.execute();
            return true;

        }catch (Exception e){
            System.out.println("Ocurrio un error al agregar estudiante: "+e.getMessage());
        }//Fin catch
        finally {
            try {
                con.close();
            }catch (Exception e){
                System.out.println("Ocurrio un error al cerrar la conexion: "+e.getMessage());
            }//Fin catch
        }//Fin finally
        return false;
    }//fin metodo agregar estudiante

    //Metodo para modificar estudiante
    public boolean modificarEstudiante(Estudiante estudiante){
        PreparedStatement ps;
        Connection con = getConexion();
        String sql = "UPDATE estudiantes2025 SET nombre=?, apellido=?, telefono=?, email=? WHERE id_estudiantes2025=?";
        try{
            ps = con.prepareStatement(sql);
            ps.setString(1, estudiante.getNombre());
            ps.setString(2, estudiante.getApellido());
            ps.setString(3, estudiante.getTelefono());
            ps.setString(4, estudiante.getEmail());
            ps.setInt(5, estudiante.getIdEstudiante());
            ps.execute();
            return true;
        }catch (Exception e){
            System.out.println("Ocurrio un error al modificar estudiante: "+e.getMessage());
        }//fin catch
        finally {
            try {
                con.close();
            }catch (Exception e){
                System.out.println("Ocurrio un error al cerrar la coneccion: "+e.getMessage());
            }//fin catch
        }//fin finally
        return false;
    }//fin del metodo modificaciones

    public boolean eliminarEstudiante (Estudiante estudiante){
        PreparedStatement ps;
        Connection con = getConexion();
        String sql = "DELETE FROM estudiantes2025 WHERE id_estudiantes2025=?";
        try {
            ps = con.prepareStatement (sql);
            ps.setInt(1,estudiante.getIdEstudiante());
            ps.execute();
            return true;
        } catch (Exception e) {
            System.out.println("Error al eliminar estudiante: " + e.getMessage());
        }
        finally {
            try {
                con.close();
            } catch (Exception e) {
                System.out.println("Error al cerra la conexión: "+e.getMessage());
            }
        }
        return false;
    }


    // public static void main(String[] args) {

    //     var estudianteDAO = new EstudianteDAO();

    //     // //Modificar estudiante
    //     // var estudianteModificado = new Estudiante(1, "Juan Carlos", "Juarez", "26466666", "jcjuarez@mail.com");
    //     // var modificado = estudianteDAO.modificarEstudiante(estudianteModificado);
    //     // if(modificado)
    //     //     System.out.println("Estudiante modificado" + estudianteModificado);
    //     // else
    //     //     System.out.println("No se modifico el estudiante" + estudianteModificado);

    //     // //Listar los estudiantes
    //     // System.out.println("Listado de estudiantes");
    //     // List<Estudiante> estudiantes = estudianteDAO.listarEstudiantes();
    //     // estudiantes.forEach(System.out::println); //Funcion lambda

    //     // // Agregar estudiante
    //     // var nuevoEstudiante = new Estudiante("Carlos", "Lara", "2645555", "cLara@mail.com");
    //     // var agregado = estudianteDAO.agregarEstudiante(nuevoEstudiante);
    //     // if(agregado)
    //     //     System.out.println("Estudiante agregado " + nuevoEstudiante);
    //     // else
    //     //     System.out.println("No se agrego el nuevo estudiante " + nuevoEstudiante);

    //     // //Buscar por id
    //     // var estudiante1 = new Estudiante(1);
    //     // System.out.println("Estudiantes antes de la busqueda: " + estudiante1);
    //     // var encontrado = estudianteDAO.buscarEstudiantePorId(estudiante1);
    //     // if(encontrado){
    //     //     System.out.println("Estudiante encontrado: " + estudiante1);
    //     // } else {
    //     //     System.out.println("No se encontro el estudiante " + estudiante1.getIdestudiante());
    //     // }

    //     //Eliminar estudiante
    //     var estudianteEliminar = new Estudiante(3);
    //     var eliminado = estudianteDAO.eliminarEstudiante(estudianteEliminar);
    //     if(eliminado){
    //         System.out.println("Estudiante eliminado: " + estudianteEliminar);
    //     } else {
    //         System.out.println("No se elimino el estudiante: " + estudianteEliminar);
    //     }

    //     //Listar los estudiantes
    //     System.out.println("Listado de estudiantes");
    //     List<Estudiante> estudiantes = estudianteDAO.listarEstudiantes();
    //     estudiantes.forEach(System.out::println); //Funcion lambda


    // }
}