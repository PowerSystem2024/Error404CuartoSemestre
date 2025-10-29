import React from 'react'
import { Card } from '../components/UI'

function AboutPage() {
  return (
    <div className="max-w-4xl mx-auto">
      <h1 className="text-3xl font-bold mb-6">Acerca del Proyecto</h1>

      <Card>
        <h2 className="text-2xl font-bold mb-4">Stack PERN - Gestor de Tareas</h2>
        <p className="text-gray-700 mb-4">
          Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
          Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
          Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.
        </p>
        <p className="text-gray-700 mb-6">
          Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
          Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium.
        </p>

        <h3 className="text-xl font-bold mb-3">Tecnologías Utilizadas</h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
          <div>
            <h4 className="font-bold text-lg mb-2">Frontend</h4>
            <ul className="list-disc list-inside space-y-1 text-gray-700">
              <li>React 19.1.1</li>
              <li>React Router 7.9.4</li>
              <li>Vite 7.1.12</li>
              <li>Tailwind CSS 4.1.16</li>
              <li>React Hook Form</li>
              <li>Axios</li>
            </ul>
          </div>

          <div>
            <h4 className="font-bold text-lg mb-2">Backend</h4>
            <ul className="list-disc list-inside space-y-1 text-gray-700">
              <li>Node.js</li>
              <li>Express</li>
              <li>PostgreSQL</li>
              <li>Zod (Validación)</li>
              <li>JSON Web Tokens</li>
              <li>bcrypt</li>
            </ul>
          </div>
        </div>

        <div className="bg-blue-50 p-4 rounded mt-4">
          <p className="text-sm text-gray-600">
            <strong>Stack PERN:</strong> PostgreSQL + Express + React + Node.js
          </p>
        </div>
      </Card>
    </div>
  )
}

export default AboutPage