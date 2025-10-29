import { Card, Input, Label, Button } from "../components/UI"
import { useForm } from 'react-hook-form'
import { useNavigate, useParams } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useTareas } from '../context/TareasContext.jsx'


function TareaFormPage() {
  const { register, handleSubmit, formState: { errors }, reset, watch } = useForm({
    defaultValues: {
      titulo: '',
      descripcion: ''
    }
  });
  const navigate = useNavigate();
  const { id } = useParams();
  const { errors: tareasErrors, loading, obtenerTareaById, crearNuevaTarea, actualizarTareaById } = useTareas();
  const [postErrors, setPostErrors] = useState(null);

  // Para ver los valores del formulario
  const watchAllFields = watch();
  console.log('Valores del formulario:', watchAllFields);

  useEffect(() => {
    const cargarTarea = async () => {
      if (id) {
        try {
          console.log('Cargando tarea con id:', id);
          const tarea = await obtenerTareaById(id);
          console.log('Tarea obtenida:', tarea);

          console.log('Haciendo reset con:', {
            titulo: tarea.titulo,
            descripcion: tarea.descripcion
          });
          reset({
            titulo: tarea.titulo,
            descripcion: tarea.descripcion
          });
        } catch (error) {
          console.log('Error al cargar tarea:', error);
        }
      }
    };
    cargarTarea();
  }, [id, reset]);

  const onSubmit = handleSubmit(async (data) => {
    try {
      setPostErrors(null);
      if (id) {
        // Actualizar tarea existente
        await actualizarTareaById(id, data);
        console.log('Tarea actualizada');
        navigate('/tareas');
      } else {
        // Crear nueva tarea
        await crearNuevaTarea(data);
        console.log('Tarea creada');
        navigate('/tareas');
      }
    } catch (error) {
      console.log('Error completo:', error);
      console.log('Error response:', error.response);
      if (error.response && error.response.data) {
        const errores = Array.isArray(error.response.data) ? error.response.data : [error.response.data];
        console.log('Errores procesados:', errores);
        setPostErrors(errores);
      } else {
        setPostErrors([{ message: 'Error de conexión. Verifica que el servidor esté corriendo.' }]);
      }
    }
  });

  return (
    <div>
      <Card>
        <h2 className="text-bold my-4">{id ? 'Editar Tarea' : 'Nueva Tarea'}</h2>

        {loading && <p className="text-gray-500 mb-4">Cargando tarea...</p>}

        {postErrors && postErrors.length > 0 && (
          <div className="mb-4">
            {postErrors.map((error, index) => (
              <div key={index} className='bg-red-500 text-white p-2 rounded mb-2'>
                {error.message || JSON.stringify(error)}
              </div>
            ))}
          </div>
        )}

        <form onSubmit={onSubmit}>
          <Label htmlFor="titulo">Título</Label>
          <Input
            type="text"
            placeholder="Título de la tarea"
            {...register("titulo", { required: "El título es requerido" })}
          />
          {errors.titulo && <span className='text-red-500 text-sm'>{errors.titulo.message}</span>}

          <Label htmlFor="descripcion">Descripción</Label>
          <Input
            type="text"
            placeholder="Descripción de la tarea"
            {...register("descripcion", { required: "La descripción es requerida" })}
          />
          {errors.descripcion && <span className='text-red-500 text-sm'>{errors.descripcion.message}</span>}

          <Button type="submit" disabled={loading}>
            {id ? 'Actualizar Tarea' : 'Guardar Tarea'}
          </Button>
        </form>
      </Card>
    </div>
  )
}

export default TareaFormPage