-- ============================================================
--  REVISTA DE RADIO DE RUTA · base de datos en Supabase
--  Pasos:
--  1) Crea una cuenta en supabase.com y un proyecto nuevo (plan gratis).
--  2) Menú «SQL Editor» > New query > pega TODO este archivo > Run.
--  3) Menú «Authentication > Providers > Email»: para empezar fácil, desactiva
--     «Confirm email» (si lo dejas activo, cada persona debe confirmar su correo).
--  4) Menú «Project Settings > API»: copia «Project URL» y la clave «anon / publishable».
--  5) En index.html pon esos dos datos en REV_URL y REV_KEY (arriba del bloque «Revista»),
--     o pégalos en la app: Ajustes > Revista (Supabase).
--  Nunca uses la clave «service_role» en la app.
-- ============================================================

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 2 and 40),
  created_at timestamptz not null default now()
);

create table public.posts (
  id uuid primary key default gen_random_uuid(),
  author_id uuid not null references public.profiles(id) on delete cascade,
  title text not null check (char_length(title) between 3 and 150),
  subtitle text check (char_length(subtitle) <= 200),
  cover text check (char_length(cover) <= 400000),
  body text not null check (char_length(body) between 20 and 20000),
  tracks jsonb not null default '[]'::jsonb check (jsonb_typeof(tracks) = 'array' and jsonb_array_length(tracks) <= 12),
  style jsonb not null default '{}'::jsonb check (jsonb_typeof(style) = 'object' and pg_column_size(style) < 400),
  created_at timestamptz not null default now()
);
create index posts_created_idx on public.posts (created_at desc);
create index posts_author_idx on public.posts (author_id);

create table public.comments (
  id uuid primary key default gen_random_uuid(),
  post_id uuid not null references public.posts(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);
create index comments_post_idx on public.comments (post_id, created_at);

create table public.likes (
  post_id uuid not null references public.posts(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  primary key (post_id, user_id)
);

create table public.follows (
  follower_id uuid not null references public.profiles(id) on delete cascade,
  author_id uuid not null references public.profiles(id) on delete cascade,
  primary key (follower_id, author_id),
  check (follower_id <> author_id)
);

-- Al crear una cuenta se crea su perfil con el nombre elegido.
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, name)
  values (new.id, left(coalesce(nullif(trim(new.raw_user_meta_data->>'name'), ''), 'Anónimo'), 40));
  return new;
end $$;
create trigger on_auth_user_created after insert on auth.users
  for each row execute function public.handle_new_user();

-- Seguridad: todos leen; solo tú escribes, cambias o borras lo tuyo.
alter table public.profiles enable row level security;
alter table public.posts    enable row level security;
alter table public.comments enable row level security;
alter table public.likes    enable row level security;
alter table public.follows  enable row level security;

create policy "leer perfiles"    on public.profiles for select using (true);
create policy "leer articulos"   on public.posts    for select using (true);
create policy "leer comentarios" on public.comments for select using (true);
create policy "leer likes"       on public.likes    for select using (true);
create policy "leer follows"     on public.follows  for select using (true);

create policy "escribir articulo"  on public.posts for insert to authenticated with check (author_id = auth.uid());
create policy "editar articulo"    on public.posts for update to authenticated using (author_id = auth.uid()) with check (author_id = auth.uid());
create policy "borrar articulo"    on public.posts for delete to authenticated using (author_id = auth.uid());

create policy "escribir comentario" on public.comments for insert to authenticated with check (author_id = auth.uid());
create policy "borrar comentario"   on public.comments for delete to authenticated using (author_id = auth.uid());

create policy "dar like"   on public.likes for insert to authenticated with check (user_id = auth.uid());
create policy "quitar like" on public.likes for delete to authenticated using (user_id = auth.uid());

create policy "seguir"        on public.follows for insert to authenticated with check (follower_id = auth.uid());
create policy "dejar de seguir" on public.follows for delete to authenticated using (follower_id = auth.uid());

-- Moderación: tú, como dueño, puedes borrar cualquier artículo o comentario
-- desde Supabase > Table Editor.

-- Si ya habías ejecutado una versión anterior de este archivo, ejecuta SOLO esta línea
-- para añadir los estilos de artículo (tema, tipografía, acento):
-- alter table public.posts add column if not exists style jsonb not null default '{}'::jsonb check (jsonb_typeof(style) = 'object' and pg_column_size(style) < 400);
