/*
    Using code from wiki page linked in D2L:
    https://en.wikipedia.org/wiki/Perlin_noise
    unsigned -> Integer.toUnsignedLong()
*/

public class PerlinNoise {
    // public static void main(String[] args){
    //     PerlinNoise n = new PerlinNoise();
    //     for(float i = 0; i < 10; i += .5)
    //         System.out.println(n.perlin(i, 2.0f));
    // }
    public PerlinNoise(){

    }
    float interpolate(float a0, float a1, float w){
        return (a1 - a0) * (3.0f - w * 2.0f) * w * w + a0;
    }
    Vector2 randomGradient(int ix, int iy){
        // final long w = Integer.toUnsignedLong(8 * Integer.BYTES);
        // final long s = w / 2;
        // long a = ix, b = iy;
        // a *= 3284157443L; b ^= a << s | a >> w-s;
        // b *= 1911520717L; a ^= b << s | b >> w-s;
        // a *= 2048419325L;
        // //float random = a * (3.14159265F / ~(Integer.toUnsignedLong(~0) >> 1) );
        // float random = a * (3.14159265 / 2147483648L);
        // Vector2 v = new Vector2();
        int a = ix, b = iy;
        int w = 32;
        int s = w / 2;
        a *= 3284157443L; b ^= a << s | a >> w-s;
        b *= 1911520717L; a ^= b << s | b >> w-s;
        a *= 2048419325L;
        float random = a * (3.14159265f / 2147483648L);
        Vector2 v = new Vector2();
        v.y = (float) Math.sin(random); v.x = (float) Math.cos(random);
        return v;
    }
    float dotGridGradient(int ix, int iy, float x, float y) {
        // Get gradient from integer coordinates
        Vector2 gradient = randomGradient(ix, iy);
    
        // Compute the distance vector
        float dx = x - (float)ix;
        float dy = y - (float)iy;
    
        // Compute the dot-product
        return (dx*gradient.x + dy*gradient.y);
    }

    public float perlin(float x, float y) {
        // Determine grid cell coordinates
        int x0 = (int)x;
        int x1 = x0 + 1;
        int y0 = (int)y;
        int y1 = y0 + 1;
    
        // Determine interpolation weights
        // Could also use higher order polynomial/s-curve here
        float sx = x - (float)x0;
        float sy = y - (float)y0;
    
        // Interpolate between grid point gradients
        float n0, n1, ix0, ix1, value;
    
        n0 = dotGridGradient(x0, y0, x, y);
        n1 = dotGridGradient(x1, y0, x, y);
        ix0 = interpolate(n0, n1, sx);
    
        n0 = dotGridGradient(x0, y1, x, y);
        n1 = dotGridGradient(x1, y1, x, y);
        ix1 = interpolate(n0, n1, sx);
    
        value = interpolate(ix0, ix1, sy);
        value += 0.4743f;
        if(value < 0) value = 0.0f;
        if(value > 1.0) value = 1.0f - (value - 1.0f);
        return value;
    }
}
class Vector2{
    public float x, y;
}