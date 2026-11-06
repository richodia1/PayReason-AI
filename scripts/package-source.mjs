import { execFileSync } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const files = [...new Set(execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean))].sort()
const forbidden = /(^|\/)(\.git|\.local|node_modules|target|dist|\.venv|data|attachments)(\/|$)|(^|\/)\.env(?!\.example$)|\.(pem|p12|key|zip)$/
for (const file of files) {
  if (forbidden.test(file) || file.includes('\n')) throw new Error(`Unsafe archive entry: ${file}`)
  const privateKeyMarker = ['-----BEGIN', 'PRIVATE KEY-----'].join(' ')
  if (readFileSync(resolve(root, file), 'utf8').includes(privateKeyMarker)) throw new Error(`Private key found: ${file}`)
}
const directory = resolve(root, '.local/releases')
mkdirSync(directory, { recursive: true })
const destination = resolve(directory, `payreason-ai-source-${new Date().toISOString().replaceAll(':', '-').replaceAll('.', '-')}.zip`)
if (existsSync(destination)) throw new Error('Archive already exists.')
execFileSync('zip', ['-q', '-X', destination, '-@'], { cwd: root, input: files.join('\n') + '\n' })
const contents = execFileSync('unzip', ['-Z1', destination], { encoding: 'utf8' }).trim().split('\n').sort()
if (JSON.stringify(contents) !== JSON.stringify(files)) throw new Error('Archive manifest verification failed.')
console.log(JSON.stringify({ archive: destination, fileCount: files.length, localEnvironmentExcluded: true, manualSecretReviewRequired: true }, null, 2))
