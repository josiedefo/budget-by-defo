/**
 * Regenerates every PWA icon in frontend/public/ from frontend/icon-source.svg.
 *
 * `sharp` is deliberately NOT a project dependency: it ships a native binary and
 * would be installed on every `npm ci` in the Docker build for no runtime benefit.
 * Install it only when you need to regenerate:
 *
 *   npm i -D --no-save sharp && node scripts/generate-icons.mjs
 *
 * Edit icon-source.svg, re-run, and commit the PNGs.
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import sharp from 'sharp'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const publicDir = join(root, 'public')
mkdirSync(publicDir, { recursive: true })

const rounded = readFileSync(join(root, 'icon-source.svg'), 'utf8')
// Maskable and apple icons must be full-bleed: Android and iOS apply their own
// mask, so baking in our own rounded corners would double-round the result.
const square = rounded.replace('rx="96" ry="96"', 'rx="0" ry="0"')

const render = (svg, size) =>
  sharp(Buffer.from(svg), { density: 384 }).resize(size, size).png({ compressionLevel: 9 }).toBuffer()

/** Wrap a PNG in a single-image ICO container (supported by every current browser). */
const pngToIco = (png, size) => {
  const header = Buffer.alloc(22)
  header.writeUInt16LE(0, 0)            // reserved
  header.writeUInt16LE(1, 2)            // type: icon
  header.writeUInt16LE(1, 4)            // image count
  header.writeUInt8(size, 6)            // width
  header.writeUInt8(size, 7)            // height
  header.writeUInt8(0, 8)               // palette size (0 = truecolour)
  header.writeUInt8(0, 9)               // reserved
  header.writeUInt16LE(1, 10)           // colour planes
  header.writeUInt16LE(32, 12)          // bits per pixel
  header.writeUInt32LE(png.length, 14)  // image size
  header.writeUInt32LE(22, 18)          // offset of image data
  return Buffer.concat([header, png])
}

const targets = [
  ['pwa-64x64.png', rounded, 64],
  ['pwa-192x192.png', rounded, 192],
  ['pwa-512x512.png', rounded, 512],
  ['maskable-icon-512x512.png', square, 512],
  ['apple-touch-icon-180x180.png', square, 180]
]

for (const [name, svg, size] of targets) {
  writeFileSync(join(publicDir, name), await render(svg, size))
  console.log(`  ${name}  ${size}x${size}`)
}

writeFileSync(join(publicDir, 'favicon.ico'), pngToIco(await render(rounded, 48), 48))
console.log('  favicon.ico  48x48')

writeFileSync(join(publicDir, 'favicon.svg'), rounded)
console.log('  favicon.svg')
