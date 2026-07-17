// Aashu Tarminal — glyph atlas skeleton.
// Rasterizes monospace glyphs once into a texture atlas and caches
// (codepoint -> UV rect) so the renderer can blit text without repeated
// font rasterization. Font loading (FreeType or similar) to be wired in.

#include <unordered_map>
#include <cstdint>

struct GlyphInfo {
    float u0, v0, u1, v1;
    int width, height;
};

class GlyphAtlas {
public:
    bool hasGlyph(uint32_t codepoint) const {
        return cache_.find(codepoint) != cache_.end();
    }

    const GlyphInfo *getGlyph(uint32_t codepoint) const {
        auto it = cache_.find(codepoint);
        return it == cache_.end() ? nullptr : &it->second;
    }

    // TODO: rasterize `codepoint` with the loaded font, pack into the
    // atlas texture, and insert its UV rect into cache_.
    void rasterizeAndCache(uint32_t codepoint) { (void) codepoint; }

private:
    std::unordered_map<uint32_t, GlyphInfo> cache_;
};
