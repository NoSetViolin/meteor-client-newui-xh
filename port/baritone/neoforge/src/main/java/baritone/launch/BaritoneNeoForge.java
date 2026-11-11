/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge 平台入口。
 *
 * <p>Baritone 的 API 自举走 {@code baritone.api.BaritoneAPI} 的静态初始化
 * （{@code Class.forName("baritone.BaritoneProvider").newInstance()}），
 * 不依赖任何加载器的 entrypoint 机制，因此这里只做平台标记与启动期日志，
 * 真正的初始化由 {@code MixinMinecraft} 在世界加载时触发。</p>
 */
@Mod(value = "baritone", dist = Dist.CLIENT)
public final class BaritoneNeoForge {

    public BaritoneNeoForge() {
        org.slf4j.LoggerFactory.getLogger("Baritone/NeoForge")
                .info("[Baritone/NeoForge] platform initialized, dist=CLIENT");
    }
}
