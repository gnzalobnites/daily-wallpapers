Este directorio contiene los fragments para el feed de wallpapers.

Archivos necesarios a crear manualmente (o mediante el script completo):

1. WallpaperFeedFragment.kt - Fragment principal con ViewPager2 y TabLayout
2. BaseFeedFragment.kt - Clase base para los fragments del feed
3. WeekFragment.kt - Muestra los últimos 7 wallpapers
4. WeekViewModel.kt - ViewModel para WeekFragment
5. CollectionFragment.kt - Muestra favoritos
6. CollectionViewModel.kt - ViewModel para CollectionFragment
7. CommunityFragment.kt - Muestra historial
8. CommunityViewModel.kt - ViewModel para CommunityFragment
9. WallpaperCardAdapter.kt - Adaptador para las tarjetas de wallpaper

Estos archivos deben ser creados con el contenido proporcionado en la documentación.
