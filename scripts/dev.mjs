import { spawn, spawnSync, execFileSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync, writeFileSync, openSync, closeSync, copyFileSync } from 'node:fs'
import { resolve, dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createServer } from 'node:net'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const local = join(root, '.local')
const stateFile = join(local, 'processes.json')
const command = process.argv[2] || 'start'
mkdirSync(local, { recursive: true })
if (!existsSync(join(root, '.env'))) copyFileSync(join(root, '.env.example'), join(root, '.env'))
process.loadEnvFile(join(root, '.env'))
const state = existsSync(stateFile) ? JSON.parse(readFileSync(stateFile, 'utf8')) : { processes: [] }

function owned(item) {
  try {
    const args = execFileSync('ps', ['-p', String(item.pid), '-o', 'command='], { encoding: 'utf8' })
    return args.includes(item.marker)
  } catch { return false }
}

if (command === 'stop') {
  for (const item of state.processes) if (owned(item)) process.kill(item.pid, 'SIGTERM')
  writeFileSync(stateFile, JSON.stringify({ processes: [] }))
  console.log('PayReason local application processes stopped. PostgreSQL data is retained.')
  process.exit(0)
}
if (command === 'status') {
  for (const item of state.processes) console.log(`${item.name}: ${owned(item) ? 'running' : 'stopped'}`)
  console.log(state.urls || {})
  process.exit(0)
}
if (command !== 'start') throw new Error('Use start, stop or status.')
if (state.processes.some(owned)) throw new Error('PayReason is already running. Use status or stop first.')

async function availablePort(preferred) {
  for (let port = Number(preferred); port < Number(preferred) + 30; port++) {
    const free = await new Promise(resolve => {
      const server = createServer()
      server.once('error', () => resolve(false))
      server.listen(port, '127.0.0.1', () => server.close(() => resolve(true)))
    })
    if (free) return port
  }
  throw new Error('No free local port found.')
}
const apiPort = await availablePort(process.env.API_PORT || 8180)
const aiPort = await availablePort(Math.max(Number(process.env.AI_PORT || 8181), apiPort + 1))
const webPort = await availablePort(process.env.WEB_PORT || 5180)
const jar = join(root, 'services/platform-api/target/platform-api-0.1.0-SNAPSHOT.jar')
const vite = join(root, 'apps/web/node_modules/vite/bin/vite.js')
const python = join(root, 'services/ai-service/.venv/bin/python')
if (!existsSync(jar) || !existsSync(vite) || !existsSync(python)) {
  throw new Error('Build the API, install web dependencies and create the Python virtual environment first. See README.md.')
}
let javaHome = process.env.JAVA_HOME
let java = javaHome ? join(javaHome, 'bin/java') : 'java'
const javaVersion = () => {
  const result = spawnSync(java, ['-version'], { encoding: 'utf8' })
  return Number((`${result.stderr || ''}${result.stdout || ''}`).match(/version "(\d+)/)?.[1] || 0)
}
if (javaVersion() !== 21 && process.platform === 'darwin') {
  javaHome = execFileSync('/usr/libexec/java_home', ['-v', '21'], { encoding: 'utf8' }).trim()
  java = join(javaHome, 'bin/java')
}
if (javaVersion() !== 21) throw new Error('Set JAVA_HOME to a Java 21 installation before starting PayReason.')
execFileSync('docker', ['compose', 'up', '-d', '--wait', 'postgres'], { cwd: root, stdio: 'inherit' })
const env = { ...process.env, API_PORT: String(apiPort), AI_PORT: String(aiPort),
  AI_BASE_URL: `http://127.0.0.1:${aiPort}`,
  PAYPAL_RETURN_URL: process.env.PAYPAL_RETURN_URL || `http://127.0.0.1:${webPort}/`,
  DATABASE_URL: process.env.DATABASE_URL || `jdbc:postgresql://127.0.0.1:${process.env.POSTGRES_PORT || 5544}/${process.env.POSTGRES_DB || 'payreason'}` }
const processes = []
function launch(name, executable, args, cwd, marker) {
  const log = openSync(join(local, `${name}.log`), 'a')
  const child = spawn(executable, args, { cwd, env, detached: true, stdio: ['ignore', log, log] })
  child.on('error', error => console.error(`${name}: ${error.message}`))
  child.unref(); closeSync(log)
  processes.push({ name, pid: child.pid, marker })
}
launch('api', java, ['-jar', jar], root, jar)
launch('ai', python, ['-m', 'uvicorn', 'app.main:app', '--host', '127.0.0.1', '--port', String(aiPort)], join(root, 'services/ai-service'), python)
launch('web', process.execPath, [vite, '--host', '127.0.0.1', '--port', String(webPort), '--strictPort'], join(root, 'apps/web'), vite)
const urls = { web: `http://127.0.0.1:${webPort}`, swagger: `http://127.0.0.1:${apiPort}/swagger-ui/index.html`, ai: `http://127.0.0.1:${aiPort}/docs` }
writeFileSync(stateFile, JSON.stringify({ processes, urls }, null, 2))
for (const url of [`http://127.0.0.1:${apiPort}/actuator/health`, `http://127.0.0.1:${aiPort}/health`, urls.web]) {
  let ready = false
  for (let attempt = 0; attempt < 50; attempt++) {
    try { if ((await fetch(url, { signal: AbortSignal.timeout(1000) })).ok) { ready = true; break } } catch { /* Startup may still be in progress. */ }
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  if (!ready) {
    for (const item of processes) if (owned(item)) process.kill(item.pid, 'SIGTERM')
    writeFileSync(stateFile, JSON.stringify({ processes: [] }))
    throw new Error(`Startup failed at ${url}. Check .local/*.log.`)
  }
}
console.log('PayReason AI is running:')
console.log(urls)
console.log('Local sign-in credentials are in .env. Stop with: node scripts/dev.mjs stop')
