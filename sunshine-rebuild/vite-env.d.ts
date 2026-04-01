/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
}

declare module 'bootstrap' {
  export class Modal {
    constructor(element: Element | null, options?: Record<string, unknown>);
    show(): void;
    hide(): void;
    toggle(): void;
    dispose(): void;
    static getInstance(element: Element): Modal | null;
  }
  export class Collapse {
    constructor(element: Element | null, options?: Record<string, unknown>);
    show(): void;
    hide(): void;
    toggle(): void;
  }
  export class Carousel {
    constructor(element: Element | null, options?: Record<string, unknown>);
    cycle(): void;
    pause(): void;
    next(): void;
    prev(): void;
    dispose(): void;
  }
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
