package com.example.ui.assets

data class SampleCover(val name: String, val url: String)

object SampleImages {
    val covers = listOf(
        SampleCover(
            name = "Tech & Innovation",
            url = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&auto=format&fit=crop"
        ),
        SampleCover(
            name = "Science & Cosmos",
            url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=800&auto=format&fit=crop"
        ),
        SampleCover(
            name = "Books & Literature",
            url = "https://images.unsplash.com/photo-1506880018603-83d5b814b5a6?w=800&auto=format&fit=crop"
        ),
        SampleCover(
            name = "Creative Art & Design",
            url = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=800&auto=format&fit=crop"
        ),
        SampleCover(
            name = "Health & Mindfulness",
            url = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800&auto=format&fit=crop"
        ),
        SampleCover(
            name = "Business & Workspace",
            url = "https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=800&auto=format&fit=crop"
        )
    )
}
