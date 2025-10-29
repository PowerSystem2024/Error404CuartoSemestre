import { useAuth } from "../context/AuthContext.jsx"
import { Link } from "react-router-dom"
import { Container } from "../components/UI"

function HomePage() {
  const { isAuth, user } = useAuth()

  return (
    <Container className="min-h-screen flex items-center justify-center">
      <div className="text-center space-y-8 max-w-4xl mx-auto px-4">
        {/* Hero Section */}
        <div className="space-y-4">
          <h1 className="text-6xl font-bold text-white">
            Bienvenido a <span className="text-sky-500">PERN Stack</span>
          </h1>
          <p className="text-2xl text-gray-400">
            Gestiona tus tareas de manera simple y eficiente
          </p>
        </div>

        {/* Descripción */}
        <div className="bg-zinc-900 rounded-lg p-8 border border-zinc-800">
          <p className="text-xl text-gray-300 leading-relaxed">
            Una aplicación full-stack construida con <span className="text-sky-400 font-semibold">PostgreSQL</span>,{" "}
            <span className="text-green-400 font-semibold">Express</span>,{" "}
            <span className="text-blue-400 font-semibold">React</span> y{" "}
            <span className="text-green-500 font-semibold">Node.js</span>
          </p>
        </div>

        {/* Call to Action */}
        {isAuth ? (
          <div className="space-y-4">
            <p className="text-xl text-white">
              Hola, <span className="text-sky-400 font-semibold">{user?.name}</span>! 👋
            </p>
            <Link
              to="/tareas"
              className="inline-block bg-sky-500 hover:bg-sky-600 text-white font-bold px-8 py-4 rounded-lg text-lg transition-all transform hover:scale-105"
            >
              Ver mis Tareas
            </Link>
          </div>
        ) : (
          <div className="space-y-6">
            <p className="text-xl text-gray-300">
              Comienza a organizar tu vida hoy
            </p>
            <div className="flex gap-4 justify-center">
              <Link
                to="/login"
                className="bg-sky-500 hover:bg-sky-600 text-white font-bold px-8 py-4 rounded-lg text-lg transition-all transform hover:scale-105"
              >
                Iniciar Sesión
              </Link>
              <Link
                to="/register"
                className="bg-zinc-800 hover:bg-zinc-700 text-white font-bold px-8 py-4 rounded-lg text-lg border border-zinc-700 transition-all transform hover:scale-105"
              >
                Registrarse
              </Link>
            </div>
          </div>
        )}

        {/* Features */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-8">
          <div className="bg-zinc-900 p-6 rounded-lg border border-zinc-800 hover:border-sky-500 transition-colors">
            <div className="text-4xl mb-4">📝</div>
            <h3 className="text-xl font-bold text-white mb-2">Crea Tareas</h3>
            <p className="text-gray-400">Organiza tus pendientes de forma simple</p>
          </div>
          <div className="bg-zinc-900 p-6 rounded-lg border border-zinc-800 hover:border-sky-500 transition-colors">
            <div className="text-4xl mb-4">✏️</div>
            <h3 className="text-xl font-bold text-white mb-2">Edita y Gestiona</h3>
            <p className="text-gray-400">Actualiza tus tareas en cualquier momento</p>
          </div>
          <div className="bg-zinc-900 p-6 rounded-lg border border-zinc-800 hover:border-sky-500 transition-colors">
            <div className="text-4xl mb-4">🔒</div>
            <h3 className="text-xl font-bold text-white mb-2">Seguro</h3>
            <p className="text-gray-400">Tus datos protegidos con autenticación JWT</p>
          </div>
        </div>
      </div>
    </Container>
  )
}

export default HomePage