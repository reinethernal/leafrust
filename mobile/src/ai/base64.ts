export function base64ToUint8Array(base64: string): Uint8Array {
  const cleaned = base64.replace(/^data:[^;]+;base64,/, '').replace(/\s/g, '');
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  const lookup = new Uint8Array(256);
  for (let i = 0; i < chars.length; i += 1) lookup[chars.charCodeAt(i)] = i;

  let padding = 0;
  if (cleaned.endsWith('==')) padding = 2;
  else if (cleaned.endsWith('=')) padding = 1;

  const len = cleaned.length;
  const bufferLength = (len * 3) / 4 - padding;
  const bytes = new Uint8Array(bufferLength);
  let p = 0;

  for (let i = 0; i < len; i += 4) {
    const a = lookup[cleaned.charCodeAt(i)];
    const b = lookup[cleaned.charCodeAt(i + 1)];
    const c = lookup[cleaned.charCodeAt(i + 2)];
    const d = lookup[cleaned.charCodeAt(i + 3)];
    bytes[p++] = (a << 2) | (b >> 4);
    if (p < bufferLength) bytes[p++] = ((b & 15) << 4) | (c >> 2);
    if (p < bufferLength) bytes[p++] = ((c & 3) << 6) | d;
  }
  return bytes;
}
