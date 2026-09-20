import { useEffect, useState } from 'react'
import { getApiError, mediaApi } from '../lib/api'
import { Button } from './ui'
import { useToast } from './ToastProvider'

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp']
const MAX_SIZE = 5 * 1024 * 1024

export function PosterUpload({ movieId, currentUrl, onUpdated }: { movieId: number; currentUrl: string | null; onUpdated: (url: string | null) => void }) {
  const { push } = useToast()
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(currentUrl)
  const [saving, setSaving] = useState(false)
  useEffect(() => { setPreview(currentUrl) }, [currentUrl])
  useEffect(() => () => { if (preview?.startsWith('blob:')) URL.revokeObjectURL(preview) }, [preview])

  const choose = (candidate: File | undefined) => {
    if (!candidate) return
    if (!ACCEPTED.includes(candidate.type)) { push('Use a JPG, PNG, or WEBP image.', 'error'); return }
    if (candidate.size > MAX_SIZE) { push('Poster must be 5 MB or smaller.', 'error'); return }
    if (preview?.startsWith('blob:')) URL.revokeObjectURL(preview)
    setFile(candidate)
    setPreview(URL.createObjectURL(candidate))
  }
  const upload = async () => {
    if (!file) return
    setSaving(true)
    try { const movie = await mediaApi.uploadPoster(movieId, file); onUpdated(movie.thumbnailUrl); setFile(null); push('Poster uploaded.', 'success') } catch (error) { push(getApiError(error), 'error') } finally { setSaving(false) }
  }
  const remove = async () => {
    setSaving(true)
    try { await mediaApi.removePoster(movieId); onUpdated(null); setFile(null); setPreview(null); push('Poster removed.', 'success') } catch (error) { push(getApiError(error), 'error') } finally { setSaving(false) }
  }

  return <div className="poster-upload" onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); choose(event.dataTransfer.files[0]) }}>
    <div className="poster-upload-preview">{preview ? <img src={preview} alt="Poster preview" /> : <span>Drop poster here</span>}</div>
    <div className="poster-upload-actions"><label className="btn btn-secondary cursor-pointer">Choose image<input className="sr-only" type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => choose(event.target.files?.[0])} /></label>{file && <Button type="button" onClick={upload} disabled={saving}>{saving ? 'Uploading...' : 'Upload poster'}</Button>}{currentUrl && !file && <Button type="button" variant="danger" onClick={remove} disabled={saving}>Remove</Button>}</div>
    <p className="text-xs text-slate-500">JPG, PNG or WEBP · max 5 MB · drag and drop supported</p>
  </div>
}
