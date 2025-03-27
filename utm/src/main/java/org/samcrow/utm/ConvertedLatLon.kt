/*
 * Copyright (c) 2025 Sam Crow
 *
 * This file is part of JRBPSurvey.
 *
 * JRBPSurvey is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * JRBPSurvey is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package org.samcrow.utm

import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/* Ellipsoid model constants (actual values here are for WGS84) */
private const val MAJOR_RADIUS = 6378137.0
private const val MINOR_RADIUS = 6356752.314
private const val SCALE_FACTOR = 0.9996

class ConvertedLatLon(val latitude: Double, val longitude: Double) {

    enum class Hemisphere {
        North,
        South,
    }

    companion object {
        /**
         * Converts UTM coordinates into latitude and longitude
         *
         * This implementation is based on
         * https://gist.github.com/vgrem/73451bd7273d8b5ba949c4d0fb654ec6 .
         */
        fun fromUtm(hemisphere: Hemisphere, lonZone: Int, easting: Double, northing: Double): ConvertedLatLon {
            val (adjustedEasting, adjustedNorthing) = adjustUtm(easting, northing, hemisphere)

            val cmeridian = getCentralMeridian(lonZone)
            return mapPointToLatLng(adjustedEasting, adjustedNorthing, cmeridian)
        }

        /**
         * Subtracts a constant from the northing if the hemisphere is south, and scales both the
         * easting and northing by a scale factor
         */
        private fun adjustUtm(easting: Double, northing: Double, hemisphere: Hemisphere): Pair<Double, Double> {
            val adjustedEasting = (easting - 500000) / SCALE_FACTOR
            var adjustedNorthing = northing
            if (hemisphere == Hemisphere.South) {
                adjustedNorthing -= 10000000
            }
            adjustedNorthing /= SCALE_FACTOR
            return Pair(adjustedEasting, adjustedNorthing)
        }


        /**
         * * Converts x and y coordinates in the Transverse Mercator projection to
         * a latitude/longitude pair.  Note that Transverse Mercator is not
         * the same as UTM; a scale factor is required to convert between them.
         *
         * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
         * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
         * @param x The easting of the point, in meters.
         * @param y The northing of the point, in meters.
         * @param lambda0 Longitude of the central meridian to be used, in radians.
         * @return latitude/longitude pair of coordinates
         */
        private fun mapPointToLatLng(x: Double, y: Double, lambda0: Double): ConvertedLatLon {
            /* Get the value of phif, the footpoint latitude. */

            val phif = getFootpointLatitude(y)

            /* Precalculate ep2 */
            val ep2 = ((MAJOR_RADIUS.pow(2.0) - MINOR_RADIUS.pow(2.0))
                    / MINOR_RADIUS.pow(2.0))

            /* Precalculate cos (phif) */
            val cf = cos(phif)

            /* Precalculate nuf2 */
            val nuf2 = ep2 * cf.pow(2.0)

            /* Precalculate Nf and initialize Nfpow */
            val Nf = MAJOR_RADIUS.pow(2.0) / (MINOR_RADIUS * sqrt(1 + nuf2))
            var Nfpow = Nf

            /* Precalculate tf */
            val tf = tan(phif)
            val tf2 = tf * tf
            val tf4 = tf2 * tf2

            /* Precalculate fractional coefficients for x**n in the equations
           below to simplify the expressions for latitude and longitude. */
            val x1frac = 1.0 / (Nfpow * cf)

            Nfpow *= Nf /* now equals Nf**2) */
            val x2frac = tf / (2.0 * Nfpow)

            Nfpow *= Nf /* now equals Nf**3) */
            val x3frac = 1.0 / (6.0 * Nfpow * cf)

            Nfpow *= Nf /* now equals Nf**4) */
            val x4frac = tf / (24.0 * Nfpow)

            Nfpow *= Nf /* now equals Nf**5) */
            val x5frac = 1.0 / (120.0 * Nfpow * cf)

            Nfpow *= Nf /* now equals Nf**6) */
            val x6frac = tf / (720.0 * Nfpow)

            Nfpow *= Nf /* now equals Nf**7) */
            val x7frac = 1.0 / (5040.0 * Nfpow * cf)

            Nfpow *= Nf /* now equals Nf**8) */
            val x8frac = tf / (40320.0 * Nfpow)

            /* Precalculate polynomial coefficients for x**n.
           -- x**1 does not have a polynomial coefficient. */
            val x2poly = -1.0 - nuf2

            val x3poly = -1.0 - 2 * tf2 - nuf2

            val x4poly = 5.0 + 3.0 * tf2 + 6.0 * nuf2 - 6.0 * tf2 * nuf2 - 3.0 * (nuf2 * nuf2) - 9.0 * tf2 * (nuf2 * nuf2)

            val x5poly = 5.0 + 28.0 * tf2 + 24.0 * tf4 + 6.0 * nuf2 + 8.0 * tf2 * nuf2

            val x6poly = (-61.0 - 90.0 * tf2 - 45.0 * tf4 - 107.0 * nuf2
                    + 162.0 * tf2 * nuf2)

            val x7poly = -61.0 - 662.0 * tf2 - 1320.0 * tf4 - 720.0 * (tf4 * tf2)

            val x8poly = 1385.0 + 3633.0 * tf2 + 4095.0 * tf4 + 1575 * (tf4 * tf2)

            /* Calculate latitude */
            val lat_rad = phif + x2frac * x2poly * (x * x) + x4frac * x4poly * x.pow(4.0) + x6frac * x6poly * x.pow(6.0) + x8frac * x8poly * x.pow(8.0)

            /* Calculate longitude */
            val lng_rad = lambda0 + x1frac * x + x3frac * x3poly * x.pow(3.0) + x5frac * x5poly * x.pow(5.0) + x7frac * x7poly * x.pow(7.0)

            return ConvertedLatLon(Math.toDegrees(lat_rad), Math.toDegrees(lng_rad))
        }

        /**
         * Computes the footpoint latitude for use in converting transverse
         * Mercator coordinates to ellipsoidal coordinates.
         *
         * Reference: Hoffmann-Wellenhof, B., Lichtenegger, H., and Collins, J.,
         * GPS: Theory and Practice, 3rd ed.  New York: Springer-Verlag Wien, 1994.
         * @param y The UTM northing coordinate, in meters.
         * @return The footpoint latitude, in radians.
         */
        private fun getFootpointLatitude(y: Double): Double {
            /* Precalculate n (Eq. 10.18) */

            val n = (MAJOR_RADIUS - MINOR_RADIUS) / (MAJOR_RADIUS + MINOR_RADIUS)

            /* Precalculate alpha_ (Eq. 10.22) */
            /* (Same as alpha in Eq. 10.17) */
            val alpha_: Double = (((MAJOR_RADIUS + MINOR_RADIUS) / 2.0)
                    * (1 + (n.pow(2.0) / 4) + (n.pow(4.0) / 64)))

            /* Precalculate y_ (Eq. 10.23) */
            val y_ = y / alpha_

            /* Precalculate beta_ (Eq. 10.22) */
            val beta_: Double = ((3.0 * n / 2.0) + (-27.0 * n.pow(3.0) / 32.0)
                    + (269.0 * n.pow(5.0) / 512.0))

            /* Precalculate gamma_ (Eq. 10.22) */
            val gamma_: Double = ((21.0 * n.pow(2.0) / 16.0)
                    + (-55.0 * n.pow(4.0) / 32.0))

            /* Precalculate delta_ (Eq. 10.22) */
            val delta_: Double = ((151.0 * n.pow(3.0) / 96.0)
                    + (-417.0 * n.pow(5.0) / 128.0))

            /* Precalculate epsilon_ (Eq. 10.22) */
            val epsilon_: Double = (1097.0 * n.pow(4.0) / 512.0)

            /* Now calculate the sum of the series (Eq. 10.21) */
            val result = (y_ + (beta_ * sin(2.0 * y_))
                    + (gamma_ * sin(4.0 * y_))
                    + (delta_ * sin(6.0 * y_))
                    + (epsilon_ * sin(8.0 * y_)))

            return result
        }

        /**
         * Determines the central meridian for the given UTM zone
         * @param zone An integer value designating the UTM zone, range [1,60]
         * @return The central meridian for the given UTM zone, in radians, or zero
         * if the UTM zone parameter is outside the range [1,60].
         * Range of the central meridian is the radian equivalent of [-177,+177]
         */
        private fun getCentralMeridian(zone: Int): Double {
            return Math.toRadians(-183.0 + (zone * 6.0))
        }
    }
}
