import { useEffect } from "react";
import { resolveMediaUrl } from "../lib/environment";

type SeoProps = {
  title: string;
  description: string;
  image?: string | null;
  jsonLd?: Record<string, unknown>;
};

function upsertMeta(
  attribute: "name" | "property",
  key: string,
  content: string,
) {
  let element = document.head.querySelector<HTMLMetaElement>(
    `meta[${attribute}="${key}"]`,
  );
  if (!element) {
    element = document.createElement("meta");
    element.setAttribute(attribute, key);
    document.head.appendChild(element);
  }
  element.content = content;
}

export function Seo({ title, description, image, jsonLd }: SeoProps) {
  useEffect(() => {
    document.title = title;
    upsertMeta("name", "description", description);
    upsertMeta("property", "og:title", title);
    upsertMeta("property", "og:description", description);
    upsertMeta("property", "og:type", "website");
    if (image) upsertMeta("property", "og:image", resolveMediaUrl(image)!);
    let canonical = document.head.querySelector<HTMLLinkElement>(
      "link[data-cinevora-canonical]",
    );
    if (!canonical) {
      canonical = document.createElement("link");
      canonical.rel = "canonical";
      canonical.dataset.cinevoraCanonical = "true";
      document.head.appendChild(canonical);
    }
    canonical.href = window.location.href.split("#")[0];
    let script = document.head.querySelector<HTMLScriptElement>(
      "script[data-cinevora-jsonld]",
    );
    if (jsonLd) {
      if (!script) {
        script = document.createElement("script");
        script.type = "application/ld+json";
        script.dataset.cinevoraJsonld = "true";
        document.head.appendChild(script);
      }
      script.textContent = JSON.stringify({ ...jsonLd, ...(typeof jsonLd.image === 'string' ? { image: resolveMediaUrl(jsonLd.image) } : {}) });
    } else if (script) script.remove();
  }, [description, image, jsonLd, title]);
  return <span data-seo-page="true" hidden />;
}
