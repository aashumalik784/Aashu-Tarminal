// Aashu Tarminal — minimal software renderer skeleton.
// Intended to blit a glyph-atlas-backed screen buffer into an
// ANativeWindow/Bitmap for TerminalView. Currently a structural stub —
// TerminalView.kt draws with Android's Canvas/Paint directly; swap it to
// call into this renderer once glyph_atlas.cpp is fleshed out for
// performance (avoids per-frame Canvas.drawText for large screens).

#include <android/bitmap.h>
#include <jni.h>

struct RenderCell {
    uint32_t codepoint;
    uint32_t fgColor;
    uint32_t bgColor;
};

class Renderer {
public:
    Renderer(int cols, int rows) : cols_(cols), rows_(rows) {}

    void resize(int cols, int rows) {
        cols_ = cols;
        rows_ = rows;
    }

    // TODO: draw cells_ into an AHardwareBuffer/Bitmap using the glyph
    // atlas for fast text blitting.
    void drawFrame(const RenderCell *cells) { (void) cells; }

private:
    int cols_;
    int rows_;
};
