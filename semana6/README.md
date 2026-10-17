# Proyecto final · Semana 6 · GitHub Copilot

**Alumno:** `Alejandro Elías Castañeda Ibarra` · **Usuario de GitHub:** `Alejandro761`

## 1. Qué construí

| | Feature | Especificación |
|---|---|---|
| [x] | `GET /reports/progress` — avance por proyecto | [`specs/progress.md`](../specs/progress.md) |

## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/Alejandro761/taskflow-copilot-Alejandro761/pull/5`
- **Commit del merge en `main`:** `https://github.com/Alejandro761/taskflow-copilot-Alejandro761/commit/1e5dd85b1518c88e4bbb9d8cb85cbd070c6f40ef`
- **Comentarios de Copilot code review:** `2`

## 3. Cómo lo hice

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/progress` y copié la spec a `specs/` | `git log --oneline main..feature/progress` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto: APROBADO`) |
| Tests | `mvn test` en verde | `79` |
| Comprobación REST | `verificar.ps1` con `casos-progress.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | https://github.com/Alejandro761/taskflow-copilot-Alejandro761/pull/5/changes |

## 4. Qué hizo el agente y qué corregí yo

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | `En ProgresoProyectosServiceTest, invoco al contructor de Task pero invirtió el orden al mandar los argumentos projectId y assigneeId invertidos` | `Copilot review` | `prompt: "Lee semana6/code-review.md y aplica esos comentarios de code review. Puedes cambiar los tests que agregaste en esta rama (git diff main); no modifiques los tests que ya estaban en main. Al terminar corre mvn -q test.", commit: "fix: comentarios de Copilot code review"` |

**Lo que el agente hizo bien a la primera**: `Los demás test los hizó correctamente. En el archivo corregido (ProgresoProyectosServiceTest) todos los test los hizó correctamente, solo tenia el error al llamar el constructor de Task (línea 42)`

## 5. Comprobaciones REST

```text
Repositorio: C:\Users\Lenovo\taskflow-copilot-Alejandro761
URL de la app: http://127.0.0.1:8080
Empaquetando con Maven (mvn -q package -DskipTests), tarda unos segundos...
App arrancando (PID 18360). Esperando a que /info responda...
App lista en 6 s.
[OK]    GET /tasks/overdue devuelve solo la tarea 7
[OK]    GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]    GET /projects/1/summary
[OK]    GET /projects/2/summary
[OK]    GET /projects/3/summary
[OK]    GET /projects/99/summary responde 404
[OK]    GET /projects/1/summary sin token responde 401
[OK]    GET /reports/progress devuelve los 3 proyectos con la semilla
[OK]    El proyecto 1 se llama «Plataforma TaskFlow»
[OK]    Tras pasar la tarea 1 a DONE, el proyecto 1 queda en 2/5 = 40
[OK]    GET /reports/progress sin token responde 401
App detenida (PID 18360).
[OK]    App apagada: el puerto 8080 ya no responde
RESULTADO: 12/12 OK
```

## 6. Créditos de la semana

| Qué | AI credits |
|---|---|
| Usados en octubre según github.com | `427.35` |
| Implementación con la skill (`AI Credits` del PF-2) | `5.96` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `1.43` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `1.42` |
