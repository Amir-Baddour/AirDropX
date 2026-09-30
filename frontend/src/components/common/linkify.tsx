import { Fragment } from 'react'

const URL_RE = /(https?:\/\/[^\s<]+[^\s<.,;:!?)\]'"])/g

/** Renders text with http(s) links made clickable (opened in a new tab, no referrer). */
export function Linkify({ text }: { text: string }) {
  const parts = text.split(URL_RE)
  return (
    <>
      {parts.map((part, i) =>
        i % 2 === 1 ? (
          <a key={i} href={part} target="_blank" rel="noopener noreferrer nofollow" className="break-all text-primary underline-offset-2 hover:underline">
            {part}
          </a>
        ) : (
          <Fragment key={i}>{part}</Fragment>
        ),
      )}
    </>
  )
}
